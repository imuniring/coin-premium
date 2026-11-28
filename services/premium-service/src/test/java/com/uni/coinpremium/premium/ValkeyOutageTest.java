package com.uni.coinpremium.premium;

import static org.assertj.core.api.Assertions.assertThat;

import com.uni.coinpremium.premium.application.exception.MarketDataUnavailableException;
import com.uni.coinpremium.premium.application.port.out.MarketDataRepository;
import com.uni.coinpremium.premium.domain.FxRate;
import com.uni.coinpremium.premium.domain.PriceQuote;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Valkey 장애 시 김프 조회 API 동작 (Toxiproxy로 장애 주입)
 * 2단계 완료 기준: 장애에도 조회 API가 빠르게, 정상 형식으로 응답
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "premium.backfill.cron=-",                  // 공백 보정 스케줄러 끄기
                "premium.last-known.initial-delay=PT0S",    // 기억 갱신은 바로 시작
                "premium.last-known.refresh-interval=PT1S"  // 테스트에선 1초마다
        }
)
@Import(TestcontainersConfig.class)
class ValkeyOutageTest {

    private static final Duration MAX_RESPONSE = Duration.ofSeconds(2);   // 장애 중에도 이 안에 응답해야 함

    @LocalServerPort
    int port;

    @Autowired
    Proxy valkeyProxy;

    @Autowired
    MarketDataRepository marketData;

    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void ready() throws Exception {
        // 신선한 시세·환율만 넣고 API는 부르지 않는다
        // → 기억은 갱신 스케줄러(1초 주기)가 채워야 함 (다중 인스턴스에서 API 호출이 없는 인스턴스와 같은 상황)
        Instant now = Instant.now();
        marketData.saveQuote(new PriceQuote("UPBIT", "BTC", new BigDecimal("107000000"), now));
        marketData.saveQuote(new PriceQuote("BINANCE", "BTC", new BigDecimal("79500"), now));
        marketData.saveFxRate(new FxRate(new BigDecimal("1340"), now));
        Thread.sleep(1500);   // 갱신 주기(1초)보다 조금 길게
    }

    @AfterEach
    void heal() throws Exception {
        // 다음 테스트에 장애가 남지 않게 원래대로
        for (var toxic : valkeyProxy.toxics().getAll()) {
            toxic.remove();
        }
        valkeyProxy.enable();
        awaitValkeyRecovered();
    }

    @Test
    @DisplayName("Valkey가 응답하지 않아도 2초 안에 응답한다")
    void valkeyHangs() throws Exception {
        // timeout 0 = 연결은 유지하되 응답을 영원히 보내지 않음 (docker pause와 같은 상황)
        valkeyProxy.toxics().timeout("valkey-hang", ToxicDirection.DOWNSTREAM, 0);

        long start = System.nanoTime();
        HttpResponse<String> response = getPremium();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertThat(elapsed).isLessThan(MAX_RESPONSE);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"stale\":true");
    }

    @Test
    @DisplayName("Valkey 연결이 끊겨도 마지막 값으로 200 + stale을 응답한다")
    void valkeyDown() throws Exception {
        valkeyProxy.disable();   // 연결 차단 (docker stop과 같은 상황)

        HttpResponse<String> response = getPremium();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"stale\":true");
    }

    @Test
    @DisplayName("기억해 둔 값도 없으면 503 MARKET_DATA_UNAVAILABLE (ProblemDetail)")
    void noLastKnownValue() throws Exception {
        valkeyProxy.disable();

        HttpResponse<String> response = get("/v1/coins/ETH/premium");   // 한 번도 조회한 적 없는 코인

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body()).contains("\"code\":\"MARKET_DATA_UNAVAILABLE\"");
    }

    private HttpResponse<String> getPremium() throws Exception {
        return get("/v1/coins/BTC/premium");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(5))
                .GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** Toxiproxy를 되돌려도 Valkey 클라이언트의 재연결은 잠깐 걸리므로, 실제로 명령이 통할 때까지 기다린다 */
    private void awaitValkeyRecovered() throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (true) {
            try {
                marketData.findFxRate();   // 가벼운 읽기 한 번
                return;                    // 통하면 회복 완료
            } catch (MarketDataUnavailableException e) {
                if (System.nanoTime() > deadline) {
                    throw new IllegalStateException("Valkey가 10초 안에 회복되지 않음", e);
                }
                Thread.sleep(100);
            }
        }
    }
}