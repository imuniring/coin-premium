package com.uni.coinpremium.collector.adapter.out.kafka;

import com.uni.coinpremium.collector.application.port.out.TickerPublisher;
import com.uni.coinpremium.contract.TickerEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 시세 수신과 Kafka 발행을 분리한다 (#42)
 * - publish(): WebSocket 스레드는 보관함에 넣기만 하고 바로 돌아감 (절대 블로킹하지 않음)
 * - 전담 스레드 1개: 보관함에서 순서대로 꺼내 Kafka로 전송, 실패하면 같은 체결을 다시 보냄
 * - 보관함이 꽉 차면 넘친 체결은 버리고 건수를 기록 (구간 기록 후 공백 보정에서 채움)
 */
@Primary   // WebSocket 클라이언트가 주입받는 TickerPublisher는 이 클래스
@Component
public class BufferedTickerPublisher implements TickerPublisher, SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(BufferedTickerPublisher.class);

    private static final String RECEIVED = "collector.ticker.received";
    private static final String DROPPED = "collector.ticker.dropped";
    private static final String BUFFER_SIZE = "collector.ticker.buffer.size";

    private final KafkaTickerPublisher kafka;
    private final MeterRegistry meterRegistry;
    private final Duration retryBackoff;
    private final BlockingQueue<TickerEvent> queue;
    private volatile Thread worker;

    // 거래소·코인별로 지금 열려 있는 유실 구간 (평소엔 비어 있음)
    private final ConcurrentHashMap<String, DropWindow> dropWindows = new ConcurrentHashMap<>();

    public BufferedTickerPublisher(KafkaTickerPublisher kafka, MeterRegistry meterRegistry,
                                   TickerBufferProperties properties) {
        this.kafka = kafka;
        this.meterRegistry = meterRegistry;
        this.retryBackoff = properties.retryBackoff();
        this.queue = new ArrayBlockingQueue<>(properties.capacity());
        meterRegistry.gauge(BUFFER_SIZE, queue, BlockingQueue::size);   // 지금 보관함에 쌓인 건수
    }

    // 테스트용: 지금 열려 있는 유실 구간
    Collection<DropWindow> openDropWindows() {
        return dropWindows.values();
    }

    /** WebSocket 수신 스레드에서 호출 → 보관함에 넣고 즉시 반환 */
    @Override
    public void publish(TickerEvent event) {
        String exchange = event.exchange().name();
        meterRegistry.counter(RECEIVED, "exchange", exchange).increment();
        if (queue.offer(event)) {
            closeDropWindow(event);
        } else {
            meterRegistry.counter(DROPPED, "exchange", exchange).increment();
            recordDrop(event);
        }
    }

    /** 넘친 체결을 구간에 누적 (첫 건만 로그, 이후는 조용히 구간만 늘림) */
    private void recordDrop(TickerEvent event) {
        DropWindow window = dropWindows.compute(key(event),
                (k, old) -> old == null ? DropWindow.start(event) : old.extend(event.tradedAt()));
        if (window.count() == 1) {
            log.atWarn()
                    .addKeyValue("symbol", event.symbol())
                    .addKeyValue("from", window.from())
                    .log("[kafka] 보관함 가득 참, 시세 유실 시작 - {} {}", event.exchange(), event.symbol());
        }
    }

    /** 다시 보관함에 들어가기 시작하면 열려 있던 유실 구간을 한 줄로 정리 */
    private void closeDropWindow(TickerEvent event) {
        if (dropWindows.isEmpty()) {
            return;   // 평소엔 여기서 끝 (추가 비용 거의 없음)
        }
        DropWindow window = dropWindows.remove(key(event));
        if (window != null) {
            logWindow(window);
        }
    }

    private void logWindow(DropWindow w) {
        log.atWarn()
                .addKeyValue("symbol", w.symbol())
                .addKeyValue("from", w.from())
                .addKeyValue("to", w.to())
                .addKeyValue("droppedCount", w.count())
                .log("[kafka] 시세 유실 구간 - {} {} {} ~ {} ({}건), 공백 보정 대상",
                        w.exchange(), w.symbol(), w.from(), w.to(), w.count());
    }

    private static String key(TickerEvent event) {
        return event.exchange().name() + ":" + event.symbol();
    }

    /** 유실 구간: 체결 시각(tradedAt) 기준 */
    record DropWindow(String exchange, String symbol, Instant from, Instant to, long count) {

        static DropWindow start(TickerEvent event) {
            return new DropWindow(event.exchange().name(), event.symbol(), event.tradedAt(), event.tradedAt(), 1);
        }

        DropWindow extend(Instant tradedAt) {
            return new DropWindow(exchange, symbol, from, tradedAt, count + 1);
        }
    }

    // ===== 전담 발행 스레드 =====

    @Override
    public void start() {
        worker = Thread.ofPlatform().name("ticker-publisher").start(this::run);
    }

    @Override
    public void stop() {
        dropWindows.values().forEach(this::logWindow);   // 종료 시점까지 열려 있던 유실 구간 기록
        dropWindows.clear();
        Thread w = worker;
        if (w == null) {
            return;
        }
        w.interrupt();
        try {
            w.join(Duration.ofSeconds(5));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        worker = null;
    }

    @Override
    public boolean isRunning() {
        return worker != null;
    }

    @Override
    public int getPhase() {
        return SmartLifecycle.DEFAULT_PHASE - 1;   // 리더 선출보다 먼저 시작, 나중에 종료
    }

    private void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                sendUntilAccepted(queue.take());   // take: 비어 있으면 들어올 때까지 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();   // 종료 신호 → 반복 종료
            }
        }
        if (!queue.isEmpty()) {
            log.warn("[kafka] 종료 시 미전송 시세 {}건", queue.size());
        }
    }

    /** Kafka가 받아 줄 때까지 같은 체결을 다시 보낸다 (순서 유지, 유실 없음) */
    private void sendUntilAccepted(TickerEvent event) throws InterruptedException {
        while (true) {
            // 전담 스레드엔 WebSocket의 MDC가 없으므로 거래소만 다시 붙인다
            try (var exchange = MDC.putCloseable("exchange", event.exchange().name())) {
                try {
                    kafka.publish(event);
                    return;
                } catch (RuntimeException e) {
                    log.atWarn()
                            .addKeyValue("bufferSize", queue.size())
                            .addKeyValue("delayMs", retryBackoff.toMillis())
                            .log("[kafka] 시세 전송 실패, {}ms 후 같은 체결 재시도 (대기 {}건): {}",
                                    retryBackoff.toMillis(), queue.size(), e.getMessage());
                }
            }
            Thread.sleep(retryBackoff);
        }
    }
}
