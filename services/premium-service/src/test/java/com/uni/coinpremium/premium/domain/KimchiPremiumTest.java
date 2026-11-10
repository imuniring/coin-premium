package com.uni.coinpremium.premium.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class KimchiPremiumTest {

    private static final Instant NOW = Instant.parse("2026-11-05T13:35:54Z");
    private static final BigDecimal FOREIGN = new BigDecimal("79528.57");   // Binance BTC (USDT)
    private static final BigDecimal RATE = new BigDecimal("1340.7");        // USD→KRW

    @Test
    @DisplayName("국내 가격이 더 비싸면 김프가 양수다")
    void positivePremium() {
        KimchiPremium premium = KimchiPremium.calculate(
                "BTC", new BigDecimal("107311000"), FOREIGN, RATE, NOW);

        assertEquals(new BigDecimal("0.6444"), premium.rate());
        assertEquals(0, new BigDecimal("106623953.799").compareTo(premium.foreignPriceKrw()));
    }

    @Test
    @DisplayName("국내 가격이 더 싸면 김프가 음수다 (역프)")
    void negativePremium() {
        KimchiPremium premium = KimchiPremium.calculate(
                "BTC", new BigDecimal("106000000"), FOREIGN, RATE, NOW);

        assertEquals(new BigDecimal("-0.5852"), premium.rate());
    }

    @Test
    @DisplayName("가격이 0이면 예외가 발생한다 (0으로 나누기 방지)")
    void zeroPrice() {
        assertThrows(IllegalArgumentException.class, () ->
                KimchiPremium.calculate("BTC", new BigDecimal("107311000"), BigDecimal.ZERO, RATE, NOW));
    }
}