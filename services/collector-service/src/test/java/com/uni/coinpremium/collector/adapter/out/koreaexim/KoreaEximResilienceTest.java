package com.uni.coinpremium.collector.adapter.out.koreaexim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import com.uni.coinpremium.collector.application.port.out.ExchangeRateFetchException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Resilience4j 설정(yml + 어노테이션)이 실제로 동작하는지 가짜 수출입은행 서버로 검증
 */
@SpringBootTest(
        classes = KoreaEximResilienceTest.TestConfig.class,
        properties = "resilience4j.retry.instances.koreaexim.wait-duration=10ms"   // 테스트 속도를 위해 대기만 짧게
)
class KoreaEximResilienceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 25);
    private static final String SUCCESS_BODY = """
            [{"result":1,"cur_unit":"USD","deal_bas_r":"1,330.8"}]""";

    // 테스트마다 응답 순서를 정해 두는 가짜 서버
    private static final Queue<Integer> statuses = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger hits = new AtomicInteger();
    private static final HttpServer server = startServer();

    /** 앱 전체가 아니라 테스트할 빈만 띄움 (WebSocket·스케줄러는 제외) */
    @Configuration
    @EnableAutoConfiguration   // Resilience4j·AOP·Jackson 자동 설정 사용
    @EnableConfigurationProperties(KoreaEximProperties.class)
    @Import({KoreaEximClient.class, KoreaEximResilienceLogger.class})
    static class TestConfig {
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("exchange.koreaexim.url", () -> "http://127.0.0.1:" + server.getAddress().getPort() + "/");
        registry.add("exchange.koreaexim.api-key", () -> "test-key");
    }

    @Autowired
    KoreaEximClient client;

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void reset() {
        statuses.clear();
        hits.set(0);
        // 스프링 컨텍스트를 테스트끼리 공유하므로 서킷 상태를 매번 초기화
        circuitBreakerRegistry.circuitBreaker("koreaexim").reset();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    @DisplayName("일시적 실패(5xx)는 재시도해서 성공한다")
    void retryThenSuccess() {
        statuses.addAll(java.util.List.of(503, 503, 200));

        assertThat(client.fetchUsdKrw(DATE)).isPresent();
        assertThat(hits.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("영구 실패(4xx)는 재시도하지 않는다")
    void noRetryOnPermanent() {
        statuses.add(400);

        assertThatThrownBy(() -> client.fetchUsdKrw(DATE)).isInstanceOf(ExchangeRateFetchException.class);
        assertThat(hits.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("3번 모두 일시적 실패하면 서킷이 열리고, 다음 호출은 서버에 가지 않고 차단된다")
    void circuitOpensAfterFailures() {
        statuses.addAll(java.util.List.of(503, 503, 503));

        assertThatThrownBy(() -> client.fetchUsdKrw(DATE)).isInstanceOf(ExchangeRateFetchException.class);
        assertThat(circuitBreakerRegistry.circuitBreaker("koreaexim").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);

        assertThatThrownBy(() -> client.fetchUsdKrw(DATE))
                .isInstanceOf(ExchangeRateFetchException.class)
                .hasMessageContaining("circuit open");
        assertThat(hits.get()).isEqualTo(3);   // 4번째 호출은 서버에 도달하지 않음
    }

    private static HttpServer startServer() {
        try {
            HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);   // 0 = 빈 포트 자동 선택
            s.createContext("/", exchange -> {
                hits.incrementAndGet();
                Integer polled = statuses.poll();
                int status = polled == null ? 200 : polled;
                byte[] body = (status == 200 ? SUCCESS_BODY : "error").getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            s.start();
            return s;
        } catch (IOException e) {
            throw new IllegalStateException("가짜 서버 시작 실패", e);
        }
    }
}