package com.uni.coinpremium.premium.application.service;

import com.uni.coinpremium.premium.adapter.out.memory.InMemoryMarketDataRepository;
import com.uni.coinpremium.premium.application.exception.MarketDataNotReadyException;
import com.uni.coinpremium.premium.application.exception.UnsupportedSymbolException;
import com.uni.coinpremium.premium.application.port.out.SupportedCoins;
import com.uni.coinpremium.premium.config.FreshnessProperties;
import com.uni.coinpremium.premium.domain.FxRate;
import com.uni.coinpremium.premium.domain.PriceQuote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PremiumQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T05:00:00Z");

    private final InMemoryMarketDataRepository repository = new InMemoryMarketDataRepository();
    private final SupportedCoins supportedCoins = () -> List.of("BTC", "ETH");   // 테스트용 지원 코인
    private final PremiumQueryService service = new PremiumQueryService(
            repository, supportedCoins,
            new FreshnessProperties(Duration.ofSeconds(60)), Clock.fixed(NOW, ZoneOffset.UTC));

    private void saveAll() {
        repository.saveQuote(new PriceQuote("UPBIT", "BTC", new BigDecimal("107311000"), NOW));
        repository.saveQuote(new PriceQuote("BINANCE", "BTC", new BigDecimal("79528.57"), NOW));
        repository.saveFxRate(new FxRate(new BigDecimal("1340.7"), Instant.parse("2026-11-10T15:00:00Z")));
    }

    @Test
    @DisplayName("시세와 환율이 모두 있으면 김프를 계산한다")
    void calculate() {
        saveAll();

        PremiumSnapshot snapshot = service.getPremium("BTC");

        assertEquals(new BigDecimal("0.6444"), snapshot.premium().rate());
        assertEquals(NOW, snapshot.premium().calculatedAt());
    }

    @Test
    @DisplayName("지원하지 않는 코인이면 UnsupportedSymbolException")
    void unsupported() {
        assertThrows(UnsupportedSymbolException.class, () -> service.getPremium("XRP"));
    }

    @Test
    @DisplayName("해외 시세가 없으면 무엇이 빠졌는지 담아 MarketDataNotReadyException")
    void notReady() {
        repository.saveQuote(new PriceQuote("UPBIT", "BTC", new BigDecimal("107311000"), NOW));
        repository.saveFxRate(new FxRate(new BigDecimal("1340.7"), NOW));

        var e = assertThrows(MarketDataNotReadyException.class, () -> service.getPremium("BTC"));
        assertEquals(List.of("BINANCE"), e.missing());
    }

    @Test
    @DisplayName("시세가 60초보다 오래되면 값은 주되 stale로 표시한다")
    void staleQuote() {
        repository.saveQuote(new PriceQuote("UPBIT", "BTC", new BigDecimal("107311000"), NOW.minusSeconds(61)));
        repository.saveQuote(new PriceQuote("BINANCE", "BTC", new BigDecimal("79528.57"), NOW));
        repository.saveFxRate(new FxRate(new BigDecimal("1340.7"), Instant.parse("2026-11-10T15:00:00Z")));

        PremiumSnapshot snapshot = service.getPremium("BTC");

        assertTrue(snapshot.stale());
        assertNotNull(snapshot.premium().rate());   // 값은 계산됨
    }

    @Test
    @DisplayName("환율이 오래된 것은 stale로 보지 않는다 (영업일 1회 고시)")
    void oldFxIsNotStale() {
        saveAll();   // 환율은 하루 전 고시

        assertFalse(service.getPremium("BTC").stale());
    }
}