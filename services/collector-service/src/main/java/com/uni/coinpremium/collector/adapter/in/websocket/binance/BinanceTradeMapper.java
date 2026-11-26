package com.uni.coinpremium.collector.adapter.in.websocket.binance;

import com.uni.coinpremium.contract.Exchange;
import com.uni.coinpremium.contract.QuoteCurrency;
import com.uni.coinpremium.contract.TickerEvent;
import com.uni.coinpremium.contract.registry.Coin;
import com.uni.coinpremium.contract.registry.CoinRegistry;

import java.time.Instant;

/**
 * Binance 체결 메시지 → TickerEvent 변환
 */
final class BinanceTradeMapper {

    private BinanceTradeMapper() {}

    static TickerEvent toEvent(BinanceStreamMessage.Trade trade, Instant receivedAt) {
        // "BTCUSDT" → 레지스트리에서 BTC를 찾음 (지원하지 않는 심볼이면 예외)
        Coin coin = CoinRegistry.findByMarket(Exchange.BINANCE, trade.symbol())
                .orElseThrow(() -> new IllegalArgumentException("unsupported symbol: " + trade.symbol()));

        return new TickerEvent(
                "BINANCE-" + trade.symbol() + "-" + trade.tradeId(),   // 체결 ID는 심볼별로만 유일해서 심볼 포함
                Exchange.BINANCE,
                coin.symbol(),
                QuoteCurrency.USDT,   // 현재 구독은 USDT 마켓만
                trade.price(),
                Instant.ofEpochMilli(trade.tradeTime()),
                receivedAt
        );
    }
}