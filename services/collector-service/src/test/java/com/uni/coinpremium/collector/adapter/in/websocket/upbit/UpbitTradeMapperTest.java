package com.uni.coinpremium.collector.adapter.in.websocket.upbit;

import com.uni.coinpremium.contract.Exchange;
import com.uni.coinpremium.contract.QuoteCurrency;
import com.uni.coinpremium.contract.TickerEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class UpbitTradeMapperTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-11-05T13:12:24Z");

    @Test
    @DisplayName("Upbit 체결 메시지를 TickerEvent로 변환한다")
    void toEvent() {
        UpbitTradeMessage msg = new UpbitTradeMessage(
                "trade", "KRW-BTC", new BigDecimal("107160000.00000000"),
                1793884343815L, 17938843438150000L);

        TickerEvent event = UpbitTradeMapper.toEvent(msg, RECEIVED_AT);

        assertEquals("UPBIT-17938843438150000", event.eventId());
        assertEquals(Exchange.UPBIT, event.exchange());
        assertEquals("BTC", event.symbol());
        assertEquals(QuoteCurrency.KRW, event.quote());
        assertEquals(0, new BigDecimal("107160000").compareTo(event.price()));   // 값 비교
        assertEquals(Instant.ofEpochMilli(1793884343815L), event.tradedAt());
    }

    @Test
    @DisplayName("지원하지 않는 마켓이면 예외가 발생한다")
    void unsupportedMarket() {
        UpbitTradeMessage msg = new UpbitTradeMessage(
                "trade", "BTC-ETH", new BigDecimal("0.03"), 1L, 1L);

        assertThrows(IllegalArgumentException.class,
                () -> UpbitTradeMapper.toEvent(msg, RECEIVED_AT));
    }
}