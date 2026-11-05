package com.uni.coinpremium.collector.adapter.in.websocket.binance;

import com.uni.coinpremium.contract.QuoteCurrency;
import com.uni.coinpremium.contract.TickerEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BinanceTradeMapperTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-11-05T13:30:00Z");

    @Test
    @DisplayName("BTCUSDT 체결을 BTC/USDT TickerEvent로 변환한다")
    void toEvent() {
        var trade = new BinanceStreamMessage.Trade(
                "trade", "BTCUSDT", 12345L, new BigDecimal("77012.34"), 1793884200000L);

        TickerEvent event = BinanceTradeMapper.toEvent(trade, RECEIVED_AT);

        assertEquals("BINANCE-BTCUSDT-12345", event.eventId());
        assertEquals("BTC", event.symbol());
        assertEquals(QuoteCurrency.USDT, event.quote());
        assertEquals(0, new BigDecimal("77012.34").compareTo(event.price()));
    }

    @Test
    @DisplayName("USDT 마켓이 아니면 예외가 발생한다")
    void unsupportedSymbol() {
        var trade = new BinanceStreamMessage.Trade(
                "trade", "ETHBTC", 1L, new BigDecimal("0.03"), 1L);

        assertThrows(IllegalArgumentException.class,
                () -> BinanceTradeMapper.toEvent(trade, RECEIVED_AT));
    }
}