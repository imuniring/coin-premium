package com.uni.coinpremium.collector.adapter.out.koreaexim;

import com.uni.coinpremium.contract.ExchangeRateEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class KoreaEximRateMapperTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 5);
    private static final Instant RECEIVED_AT = Instant.parse("2026-11-05T03:00:00Z");

    @Test
    @DisplayName("쉼표가 포함된 USD 매매기준율을 BigDecimal로 변환한다")
    void parseUsd() {
        KoreaEximRate[] rates = {
                new KoreaEximRate(1, "JPY(100)", "931.09"),
                new KoreaEximRate(1, "USD", "1,369.4")
        };

        ExchangeRateEvent event = KoreaEximRateMapper.toUsdKrw(rates, DATE, RECEIVED_AT).orElseThrow();

        assertEquals(0, new BigDecimal("1369.4").compareTo(event.rate()));
        assertEquals("KOREAEXIM-USD-KRW-20261105", event.eventId());
    }

    @Test
    @DisplayName("영업일이 아니면(빈 배열) empty를 반환한다")
    void emptyOnHoliday() {
        Optional<ExchangeRateEvent> result =
                KoreaEximRateMapper.toUsdKrw(new KoreaEximRate[0], DATE, RECEIVED_AT);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("오류 응답(result != 1)이면 예외가 발생한다")
    void errorResult() {
        KoreaEximRate[] rates = { new KoreaEximRate(3, null, null) };   // 3 = 인증코드 오류

        assertThrows(IllegalStateException.class,
                () -> KoreaEximRateMapper.toUsdKrw(rates, DATE, RECEIVED_AT));
    }
}