package com.uni.coinpremium.premium.application.service;

import com.uni.coinpremium.premium.adapter.out.memory.InMemoryMarketDataRepository;
import com.uni.coinpremium.premium.application.exception.MarketDataNotReadyException;
import com.uni.coinpremium.premium.application.exception.UnsupportedSymbolException;
import com.uni.coinpremium.premium.config.PremiumProperties;
import com.uni.coinpremium.premium.domain.FxRate;
import com.uni.coinpremium.premium.domain.PriceQuote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PremiumQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T05:00:00Z");

    private final InMemoryMarketDataRepository repository = new InMemoryMarketDataRepository();
    private final PremiumQueryService service = new PremiumQueryService(
            repository, new PremiumProperties(List.of("BTC", "ETH")), Clock.fixed(NOW, ZoneOffset.UTC));

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
}