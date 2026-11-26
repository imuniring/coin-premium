package com.uni.coinpremium.collector.adapter.in.websocket;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 거래소 WebSocket 공통 클라이언트
 * - 연결/종료, 프레임 버퍼링
 * - 자동 재연결 (지수 백오프 + 지터)
 * - 좀비 연결 감지 (일정 시간 수신 없으면 강제 재연결)
 */
public abstract class ExchangeWebSocketClient {

    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(60);
    private static final Duration IDLE_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration WATCHDOG_INTERVAL = Duration.ofSeconds(10);

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name(getClass().getSimpleName() + "-scheduler").factory());

    private final AtomicInteger attempt = new AtomicInteger();                 // 연속 실패 횟수
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean();      // 재연결 중복 예약 방지
    private volatile WebSocket webSocket;
    private volatile Instant lastMessageAt = Instant.now();
    private volatile String connectionId;   // 현재 연결 ID (좀비 감지 로그용)
    private volatile boolean active;         // 지금 수집 중인가 (리더일 때 true)
    private volatile int session;            // 켤 때마다 증가. 이전 세대의 연결·재연결 작업을 무시하는 데 사용

    // ===== 자식 클래스가 구현하는 부분 =====

    /** 로그용 거래소 이름 */
    protected abstract String name();

    /** 접속 주소 */
    protected abstract URI uri();

    /** 연결 직후 보낼 구독 메시지 (없으면 null) */
    protected String subscribeMessage() {
        return null;
    }

    /** 완성된 메시지 1건 처리 (변환 + 발행) */
    protected abstract void onMessage(String message, Instant receivedAt);

    // ===== 생명주기 =====

    /** 좀비 감지 감시는 한 번만 시작해 두고, 수집 중일 때만 동작 */
    @PostConstruct
    void initWatchdog() {
        scheduler.scheduleWithFixedDelay(this::checkIdle,
                WATCHDOG_INTERVAL.toSeconds(), WATCHDOG_INTERVAL.toSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 수집 시작 (여러 번 호출해도 안전)
     * 지금은 앱 시작 시 호출, 이후 리더가 됐을 때 호출하도록 바뀜
     */
    @EventListener(ApplicationReadyEvent.class)
    public synchronized void activate() {
        if (active) {
            return;
        }
        active = true;
        int s = ++session;
        attempt.set(0);
        lastMessageAt = Instant.now();
        log.info("[{}] 수집 시작", name());
        connect(s);
    }

    /** 수집 중단 (다시 activate 가능). 리더를 잃었을 때 호출 */
    public synchronized void deactivate() {
        if (!active) {
            return;
        }
        active = false;
        session++;                 // 이전 세대의 예약된 재연결·진행 중인 연결은 모두 무시됨
        reconnectScheduled.set(false);
        WebSocket ws = webSocket;
        webSocket = null;
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "deactivated");
        }
        log.info("[{}] 수집 중단", name());
    }

    /** 앱 종료: 수집 중단 + 내부 스레드 정리 (이후 재시작 불가) */
    @PreDestroy
    public void shutdown() {
        deactivate();
        scheduler.shutdownNow();
    }

    /** 이 작업이 지금 세대의 것인가 */
    private boolean isCurrent(int s) {
        return active && s == session;
    }

    // ===== 연결 / 재연결 =====

    private void connect(int s) {
        if (!isCurrent(s)) {
            return;   // 이미 꺼졌거나 다음 세대로 넘어감
        }
        reconnectScheduled.set(false);
        String connId = UUID.randomUUID().toString();   // 연결 시도마다 새 ID
        connectionId = connId;

        withMdc(connId, () -> {
            log.atInfo()
                    .addKeyValue("attempt", attempt.get())
                    .log("[{}] 연결 시도 (연속 실패 {}회)", name(), attempt.get());

            httpClient.newWebSocketBuilder()
                    .buildAsync(uri(), new Listener(connId, s))
                    .thenAccept(ws -> {
                        if (isCurrent(s)) {
                            webSocket = ws;
                        } else {
                            ws.abort();   // 연결되는 사이 꺼졌으면 바로 끊음
                        }
                    })
                    .exceptionally(e -> {
                        // 연결 실패 콜백도 다른 스레드에서 실행되므로 다시 MDC를 넣는다
                        withMdc(connId, () -> {
                            log.warn("[{}] 연결 실패: {}", name(), e.getMessage());
                            scheduleReconnect(s);
                        });
                        return null;
                    });
        });
    }

    private void scheduleReconnect(int s) {
        // 종료 중이거나, 이미 재연결이 예약돼 있으면 무시 (onClose + onError 중복 호출 대비)
        if (!isCurrent(s) || !reconnectScheduled.compareAndSet(false, true)) {
            return;
        }
        long delayMs = backoffMillis(attempt.getAndIncrement());
        log.atWarn()
                .addKeyValue("delayMs", delayMs)
                .log("[{}] {}ms 후 재연결 예정", name(), delayMs);
        scheduler.schedule(() -> connect(s), delayMs, TimeUnit.MILLISECONDS);
    }

    // 1s → 2s → 4s ... 최대 60s, 여기에 0~20% 랜덤 지터 추가
    private long backoffMillis(int failures) {
        long base = Math.min(MAX_BACKOFF.toMillis(), INITIAL_BACKOFF.toMillis() << Math.min(failures, 16));
        long jitter = ThreadLocalRandom.current().nextLong(base / 5 + 1);
        return base + jitter;
    }

    // 좀비 연결 감지: 연결은 살아있는데 데이터가 안 오는 상태
    private void checkIdle() {
        WebSocket ws = webSocket;
        if (!active || ws == null || reconnectScheduled.get()) {
            return;
        }
        int s = session;
        Duration idle = Duration.between(lastMessageAt, Instant.now());
        if (idle.compareTo(IDLE_TIMEOUT) > 0) {
            withMdc(connectionId, () -> {
                log.atWarn()
                        .addKeyValue("idleSeconds", idle.toSeconds())
                        .log("[{}] {}초간 수신 없음 → 강제 재연결", name(), idle.toSeconds());
                ws.abort();
                scheduleReconnect(s);
            });
        }
    }

    /**
     * 거래소·연결 ID를 MDC에 넣은 채로 작업 실행
     * WebSocket 콜백은 HttpClient 내부 스레드에서 돌아 스레드가 계속 바뀌므로,
     * 콜백 입구마다 넣고 끝나면 자동으로 지운다.
     */
    private void withMdc(String connId, Runnable action) {
        try (var exchange = MDC.putCloseable("exchange", name().toUpperCase(Locale.ROOT));
             var conn = MDC.putCloseable("connectionId", connId)) {
            action.run();
        }
    }

    // ===== 메시지 수신 (연결마다 새로 생성되므로 버퍼도 연결마다 초기화됨) =====

    private final class Listener implements WebSocket.Listener {

        private final String connId;   // 이 연결의 ID (Listener는 연결마다 새로 생성됨)
        private final int session;   // 이 연결이 속한 세대
        private final StringBuilder textBuffer = new StringBuilder();
        private final ByteArrayOutputStream binaryBuffer = new ByteArrayOutputStream();

        private Listener(String connId, int session) {
            this.connId = connId;
            this.session = session;
        }

        @Override
        public void onOpen(WebSocket ws) {
            if (!isCurrent(session)) {
                ws.abort();   // 이전 세대의 연결이 뒤늦게 열림 → 바로 끊음
                return;
            }
            withMdc(connId, () -> {
                log.info("[{}] 연결 성공", name());
                lastMessageAt = Instant.now();
                String subscribe = subscribeMessage();
                if (subscribe != null) {
                    ws.sendText(subscribe, true);
                }
                ws.request(1);
            });
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            withMdc(connId, () -> {
                textBuffer.append(data);
                if (last) {
                    dispatch(textBuffer.toString());
                    textBuffer.setLength(0);
                }
                ws.request(1);
            });
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
            withMdc(connId, () -> {
                byte[] bytes = new byte[data.remaining()];
                data.get(bytes);
                binaryBuffer.writeBytes(bytes);
                if (last) {
                    dispatch(binaryBuffer.toString(StandardCharsets.UTF_8));
                    binaryBuffer.reset();
                }
                ws.request(1);
            });
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            withMdc(connId, () -> {
                log.atWarn()
                        .addKeyValue("statusCode", statusCode)
                        .addKeyValue("reason", reason)
                        .log("[{}] 연결 종료 - code={}, reason={}", name(), statusCode, reason);
                scheduleReconnect(session);
            });
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            withMdc(connId, () -> {
                log.error("[{}] 연결 에러", name(), error);
                scheduleReconnect(session);
            });
        }

        private void dispatch(String message) {
            Instant receivedAt = Instant.now();
            lastMessageAt = receivedAt;
            attempt.set(0);   // 데이터가 실제로 들어와야 정상 연결로 보고 백오프 초기화
            try {
                onMessage(message, receivedAt);
            } catch (Exception e) {
                // 불량 메시지 1건은 버리고 연결은 유지
                log.warn("[{}] 메시지 처리 실패 - message={}", name(), message, e);
            }
        }
    }
}