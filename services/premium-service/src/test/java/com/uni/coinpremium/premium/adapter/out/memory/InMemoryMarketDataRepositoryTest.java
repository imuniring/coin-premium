package com.uni.coinpremium.premium.adapter.out.memory;

import com.uni.coinpremium.premium.domain.FxRate;
import com.uni.coinpremium.premium.domain.PriceQuote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryMarketDataRepositoryTest {

    private final InMemoryMarketDataRepository repository = new InMemoryMarketDataRepository();

    private static PriceQuote upbitBtc(String price, String tradedAt) {
        return new PriceQuote("UPBIT", "BTC", new BigDecimal(price), Instant.parse(tradedAt));
    }

    @Test
    @DisplayName("더 최신 시세가 오면 교체한다")
    void replaceWithNewer() {
        repository.saveQuote(upbitBtc("100", "2026-11-06T01:00:00Z"));
        repository.saveQuote(upbitBtc("200", "2026-11-06T01:00:01Z"));

        assertEquals(new BigDecimal("200"), repository.findQuote("UPBIT", "BTC").orElseThrow().price());
    }

    @Test
    @DisplayName("순서가 뒤바뀌어 옛날 시세가 와도 최신값을 유지한다")
    void ignoreOlder() {
        repository.saveQuote(upbitBtc("200", "2026-11-06T01:00:01Z"));
        repository.saveQuote(upbitBtc("100", "2026-11-06T01:00:00Z"));   // 늦게 도착한 옛날 값

        assertEquals(new BigDecimal("200"), repository.findQuote("UPBIT", "BTC").orElseThrow().price());
    }

    @Test
    @DisplayName("거래소별로 따로 저장한다")
    void separateByExchange() {
        repository.saveQuote(upbitBtc("107311000", "2026-11-06T01:00:00Z"));
        repository.saveQuote(new PriceQuote("BINANCE", "BTC", new BigDecimal("79528.57"),
                Instant.parse("2026-11-06T01:00:00Z")));

        assertEquals(new BigDecimal("107311000"), repository.findQuote("UPBIT", "BTC").orElseThrow().price());
        assertEquals(new BigDecimal("79528.57"), repository.findQuote("BINANCE", "BTC").orElseThrow().price());
    }

    @Test
    @DisplayName("환율도 더 최신일 때만 교체한다")
    void fxRateKeepsNewest() {
        repository.saveFxRate(new FxRate(new BigDecimal("1340.7"), Instant.parse("2026-11-04T15:00:00Z")));   // 11/5
        repository.saveFxRate(new FxRate(new BigDecimal("1330.0"), Instant.parse("2026-11-03T15:00:00Z")));   // 11/4 (옛날)

        assertEquals(new BigDecimal("1340.7"), repository.findFxRate().orElseThrow().rate());
    }
}