package com.uni.coinpremium.premium.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PriceQuoteTest {

    private static final Instant NOW = Instant.parse("2026-11-27T00:00:00Z");
    private static final Duration MAX_AGE = Duration.ofSeconds(60);

    @Test
    @DisplayName("정확히 60초 전 체결은 아직 신선하다")
    void exactlyMaxAgeIsFresh() {
        assertFalse(quoteAt(NOW.minus(MAX_AGE)).isStale(NOW, MAX_AGE));
    }

    @Test
    @DisplayName("60초를 1밀리초라도 넘기면 오래된 시세다")
    void overMaxAgeIsStale() {
        assertTrue(quoteAt(NOW.minus(MAX_AGE).minusMillis(1)).isStale(NOW, MAX_AGE));
    }

    private static PriceQuote quoteAt(Instant tradedAt) {
        return new PriceQuote("UPBIT", "BTC", new BigDecimal("1000"), tradedAt);
    }
}