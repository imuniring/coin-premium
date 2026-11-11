package com.uni.coinpremium.premium.application.service;

import com.uni.coinpremium.premium.adapter.out.memory.InMemoryMarketDataRepository;
import com.uni.coinpremium.premium.application.port.out.PremiumSnapshotRepository;
import com.uni.coinpremium.premium.config.PremiumProperties;
import com.uni.coinpremium.premium.domain.FxRate;
import com.uni.coinpremium.premium.domain.PriceQuote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PremiumSnapshotServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T05:01:37.500Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryMarketDataRepository market = new InMemoryMarketDataRepository();
    private final PremiumProperties properties = new PremiumProperties(List.of("BTC", "ETH"));
    private final PremiumQueryService queryService = new PremiumQueryService(market, properties, CLOCK);

    private record Saved(String symbol, Instant capturedAt) {}
    private final List<Saved> saved = new ArrayList<>();

    private PremiumSnapshotService serviceWith(PremiumSnapshotRepository repository) {
        return new PremiumSnapshotService(queryService, repository, properties, CLOCK);
    }

    private void ready(String symbol) {
        market.saveQuote(new PriceQuote("UPBIT", symbol, new BigDecimal("1000"), NOW));
        market.saveQuote(new PriceQuote("BINANCE", symbol, new BigDecimal("0.7"), NOW));
    }

    @Test
    @DisplayName("기준 시각을 분 단위로 맞추고, 데이터 준비 전인 코인은 건너뛴다")
    void captureReadySymbolsOnly() {
        market.saveFxRate(new FxRate(new BigDecimal("1400"), NOW));
        ready("BTC");                    // ETH는 시세 없음

        serviceWith((s, at) -> saved.add(new Saved(s.premium().symbol(), at))).captureAll();

        assertEquals(List.of(new Saved("BTC", Instant.parse("2026-11-11T05:01:00Z"))), saved);
    }

    @Test
    @DisplayName("한 코인 저장이 실패해도 다른 코인은 저장한다")
    void continueOnFailure() {
        market.saveFxRate(new FxRate(new BigDecimal("1400"), NOW));
        ready("BTC");
        ready("ETH");

        serviceWith((s, at) -> {
            if (s.premium().symbol().equals("BTC")) {
                throw new IllegalStateException("DB 장애");
            }
            return saved.add(new Saved(s.premium().symbol(), at));
        }).captureAll();

        assertEquals(1, saved.size());
        assertEquals("ETH", saved.get(0).symbol());
    }
}