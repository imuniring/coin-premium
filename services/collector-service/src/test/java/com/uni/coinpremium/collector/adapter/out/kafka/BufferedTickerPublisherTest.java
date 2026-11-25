package com.uni.coinpremium.collector.adapter.out.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.uni.coinpremium.contract.Exchange;
import com.uni.coinpremium.contract.QuoteCurrency;
import com.uni.coinpremium.contract.TickerEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BufferedTickerPublisherTest {

    private static final Instant BASE = Instant.parse("2026-11-25T00:00:00Z");

    private final KafkaTickerPublisher kafka = mock(KafkaTickerPublisher.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private BufferedTickerPublisher publisher;

    @AfterEach
    void tearDown() {
        if (publisher != null) {
            publisher.stop();
        }
    }

    @Test
    @DisplayName("Kafka 전송이 멈춰 있어도 publish()는 기다리지 않고 바로 돌아온다")
    void publishDoesNotBlockWhenKafkaHangs() {
        CountDownLatch never = new CountDownLatch(1);
        doAnswer(inv -> {
            never.await();   // Kafka 장애 흉내: 영원히 멈춤 (stop()의 인터럽트로만 풀림)
            return null;
        }).when(kafka).publish(any());
        publisher = create(1_000);
        publisher.start();

        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            publisher.publish(event(i));
        }
        long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

        assertThat(elapsedMs).isLessThan(500);   // 100건을 넣는 데 막힘 없음
    }

    @Test
    @DisplayName("전송에 실패하면 같은 체결을 다시 보내고, 순서를 유지한다")
    void retriesSameEventInOrder() throws InterruptedException {
        AtomicInteger calls = new AtomicInteger();
        List<TickerEvent> sent = new CopyOnWriteArrayList<>();
        doAnswer(inv -> {
            if (calls.incrementAndGet() <= 2) {
                throw new RuntimeException("kafka down");   // 처음 두 번은 실패
            }
            sent.add(inv.getArgument(0));
            return null;
        }).when(kafka).publish(any());
        publisher = create(1_000);
        TickerEvent first = event(1);
        TickerEvent second = event(2);
        publisher.publish(first);
        publisher.publish(second);

        publisher.start();
        await(() -> sent.size() == 2);

        assertThat(sent).containsExactly(first, second);   // 실패한 first가 버려지지 않고 먼저 나감
        assertThat(calls.get()).isEqualTo(4);               // first 3번(실패 2 + 성공 1) + second 1번
    }

    @Test
    @DisplayName("보관함이 넘치면 버린 건수와 체결 시각 기준 유실 구간을 기록하고, 다시 들어가면 구간을 닫는다")
    void recordsDropWindowOnOverflow() throws InterruptedException {
        publisher = create(2);   // 일부러 아주 작게, 전담 스레드도 아직 시작 안 함

        for (int i = 1; i <= 5; i++) {
            publisher.publish(event(i));   // 1, 2는 들어가고 3, 4, 5는 넘침
        }

        assertThat(registry.counter("collector.ticker.dropped", "exchange", "BINANCE").count()).isEqualTo(3);
        assertThat(publisher.openDropWindows()).singleElement().satisfies(w -> {
            assertThat(w.from()).isEqualTo(event(3).tradedAt());
            assertThat(w.to()).isEqualTo(event(5).tradedAt());
            assertThat(w.count()).isEqualTo(3);
        });

        // 전담 스레드를 시작해 보관함을 비운 뒤 다시 넣으면 구간이 닫힘
        publisher.start();
        await(() -> registry.get("collector.ticker.buffer.size").gauge().value() == 0);
        publisher.publish(event(6));

        assertThat(publisher.openDropWindows()).isEmpty();
    }

    private BufferedTickerPublisher create(int capacity) {
        return new BufferedTickerPublisher(kafka, registry,
                new TickerBufferProperties(capacity, Duration.ofMillis(10)));   // 재시도 간격은 짧게
    }

    // i초에 체결된 BINANCE BTC 시세 (eventId는 매번 새로 생성되지만 tradedAt은 i로 고정)
    private static TickerEvent event(int i) {
        return TickerEvent.of(Exchange.BINANCE, "BTC", QuoteCurrency.USDT,
                BigDecimal.ONE, BASE.plusSeconds(i), BASE.plusSeconds(i));
    }

    // 다른 스레드의 작업이 끝날 때까지 최대 3초 기다림
    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("3초 안에 조건이 충족되지 않음");
            }
            Thread.sleep(10);
        }
    }
}