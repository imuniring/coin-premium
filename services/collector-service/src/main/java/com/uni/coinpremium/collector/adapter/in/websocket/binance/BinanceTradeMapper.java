package com.uni.coinpremium.collector.adapter.in.websocket.binance;

import com.uni.coinpremium.contract.Exchange;
import com.uni.coinpremium.contract.QuoteCurrency;
import com.uni.coinpremium.contract.TickerEvent;

import java.time.Instant;

/**
 * Binance 체결 메시지 → TickerEvent 변환
 */
final class BinanceTradeMapper {

    private BinanceTradeMapper() {}

    static TickerEvent toEvent(BinanceStreamMessage.Trade trade, Instant receivedAt) {
        // "BTCUSDT" → 끝이 USDT인지 확인하고 앞부분을 심볼로 사용
        QuoteCurrency quote = QuoteCurrency.USDT;
        String symbol = trade.symbol();
        if (!symbol.endsWith(quote.name()) || symbol.length() == quote.name().length()) {
            throw new IllegalArgumentException("unsupported symbol: " + symbol);
        }
        String base = symbol.substring(0, symbol.length() - quote.name().length());

        return new TickerEvent(
                "BINANCE-" + symbol + "-" + trade.tradeId(),   // 체결 ID는 심볼별로만 유일해서 심볼 포함
                Exchange.BINANCE,
                base,
                quote,
                trade.price(),
                Instant.ofEpochMilli(trade.tradeTime()),
                receivedAt
        );
    }
}