package com.uni.coinpremium.collector.adapter.in.websocket;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private volatile boolean stopping;

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

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        scheduler.scheduleWithFixedDelay(this::checkIdle,
                WATCHDOG_INTERVAL.toSeconds(), WATCHDOG_INTERVAL.toSeconds(), TimeUnit.SECONDS);
        connect();
    }

    @PreDestroy
    public void stop() {
        stopping = true;   // 이후 재연결 시도 차단
        WebSocket ws = webSocket;
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
        scheduler.shutdownNow();
    }

    // ===== 연결 / 재연결 =====

    private void connect() {
        if (stopping) {
            return;
        }
        reconnectScheduled.set(false);
        log.info("[{}] 연결 시도 (연속 실패 {}회)", name(), attempt.get());

        httpClient.newWebSocketBuilder()
                .buildAsync(uri(), new Listener())
                .thenAccept(ws -> webSocket = ws)
                .exceptionally(e -> {
                    log.warn("[{}] 연결 실패: {}", name(), e.getMessage());
                    scheduleReconnect();
                    return null;
                });
    }

    private void scheduleReconnect() {
        // 종료 중이거나, 이미 재연결이 예약돼 있으면 무시 (onClose + onError 중복 호출 대비)
        if (stopping || !reconnectScheduled.compareAndSet(false, true)) {
            return;
        }
        long delayMs = backoffMillis(attempt.getAndIncrement());
        log.warn("[{}] {}ms 후 재연결 예정", name(), delayMs);
        scheduler.schedule(this::connect, delayMs, TimeUnit.MILLISECONDS);
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
        if (stopping || ws == null || reconnectScheduled.get()) {
            return;
        }
        Duration idle = Duration.between(lastMessageAt, Instant.now());
        if (idle.compareTo(IDLE_TIMEOUT) > 0) {
            log.warn("[{}] {}초간 수신 없음 → 강제 재연결", name(), idle.toSeconds());
            ws.abort();
            scheduleReconnect();
        }
    }

    // ===== 메시지 수신 (연결마다 새로 생성되므로 버퍼도 연결마다 초기화됨) =====

    private final class Listener implements WebSocket.Listener {

        private final StringBuilder textBuffer = new StringBuilder();
        private final ByteArrayOutputStream binaryBuffer = new ByteArrayOutputStream();

        @Override
        public void onOpen(WebSocket ws) {
            log.info("[{}] 연결 성공", name());
            lastMessageAt = Instant.now();
            String subscribe = subscribeMessage();
            if (subscribe != null) {
                ws.sendText(subscribe, true);
            }
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (last) {
                dispatch(textBuffer.toString());
                textBuffer.setLength(0);
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            binaryBuffer.writeBytes(bytes);
            if (last) {
                dispatch(binaryBuffer.toString(StandardCharsets.UTF_8));
                binaryBuffer.reset();
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            log.warn("[{}] 연결 종료 - code={}, reason={}", name(), statusCode, reason);
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            log.error("[{}] 연결 에러", name(), error);
            scheduleReconnect();
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