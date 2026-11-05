package com.uni.coinpremium.contract;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ExchangeRateEventTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-11-05T02:00:00Z");
    private static final Instant RECEIVED_AT = Instant.parse("2026-11-05T02:00:05Z");

    @Test
    @DisplayName("정상 값이면 이벤트가 생성된다")
    void create() {
        ExchangeRateEvent event = ExchangeRateEvent.of(
                FiatCurrency.USD, FiatCurrency.KRW, new BigDecimal("1380.50"),
                RateSource.KOREAEXIM, PUBLISHED_AT, RECEIVED_AT);

        assertNotNull(event.eventId());
        assertEquals(new BigDecimal("1380.50"), event.rate());
        assertEquals(RateSource.KOREAEXIM, event.source());
    }

    @Test
    @DisplayName("기준 통화와 대상 통화가 같으면 예외가 발생한다")
    void sameCurrency() {
        assertThrows(IllegalArgumentException.class, () ->
                ExchangeRateEvent.of(FiatCurrency.USD, FiatCurrency.USD, new BigDecimal("1"),
                        RateSource.KOREAEXIM, PUBLISHED_AT, RECEIVED_AT));
    }

    @Test
    @DisplayName("환율이 0 이하면 예외가 발생한다")
    void nonPositiveRate() {
        assertThrows(IllegalArgumentException.class, () ->
                ExchangeRateEvent.of(FiatCurrency.USD, FiatCurrency.KRW, new BigDecimal("-1"),
                        RateSource.KOREAEXIM, PUBLISHED_AT, RECEIVED_AT));
    }
}