package com.uni.coinpremium.contract;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TickerEventTest {

    // 테스트마다 같은 시각을 쓰도록 고정 (Instant.now()는 실행할 때마다 달라짐)
    private static final Instant TRADED_AT = Instant.parse("2026-11-05T12:00:00Z");
    private static final Instant RECEIVED_AT = Instant.parse("2026-11-05T12:00:01Z");

    @Test
    @DisplayName("정상 값이면 이벤트가 생성된다")
    void create() {
        // given & when
        TickerEvent event = TickerEvent.of(
                Exchange.UPBIT, "BTC", QuoteCurrency.KRW,
                new BigDecimal("150000000"), TRADED_AT, RECEIVED_AT);

        // then
        assertNotNull(event.eventId());           // ID가 자동 발급됐는지
        assertEquals("BTC", event.symbol());
        assertEquals(new BigDecimal("150000000"), event.price());
        assertEquals(TRADED_AT, event.tradedAt());
    }

    @Test
    @DisplayName("가격이 0이면 예외가 발생한다.")
    void zeroPrice() {
        assertThrows(IllegalArgumentException.class, () ->
                TickerEvent.of(Exchange.UPBIT, "BTC", QuoteCurrency.KRW,
                        BigDecimal.ZERO, TRADED_AT, RECEIVED_AT));   // ← 여기만 0
    }

    @Test
    @DisplayName("심볼이 공백이면 예외가 발생한다")
    void blankSymbol() {
        assertThrows(IllegalArgumentException.class, () ->
                TickerEvent.of(Exchange.UPBIT, " ", QuoteCurrency.KRW,   // ← 여기만 공백
                        new BigDecimal("150000000"), TRADED_AT, RECEIVED_AT));
    }
}