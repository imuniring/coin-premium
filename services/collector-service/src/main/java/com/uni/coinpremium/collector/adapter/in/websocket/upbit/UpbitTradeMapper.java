package com.uni.coinpremium.collector.adapter.in.websocket.upbit;

import com.uni.coinpremium.contract.Exchange;
import com.uni.coinpremium.contract.QuoteCurrency;
import com.uni.coinpremium.contract.TickerEvent;

import java.time.Instant;

/**
 * Upbit 체결 메시지 → TickerEvent 변환
 */
final class UpbitTradeMapper {

    private UpbitTradeMapper() {}

    static TickerEvent toEvent(UpbitTradeMessage msg, Instant receivedAt) {
        // "KRW-BTC" → [KRW, BTC]
        String[] parts = msg.code().split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("unexpected market code: " + msg.code());
        }

        return new TickerEvent(
                "UPBIT-" + msg.sequentialId(),            // 같은 체결은 항상 같은 ID (중복 판단용)
                Exchange.UPBIT,
                parts[1],                                  // BTC
                QuoteCurrency.valueOf(parts[0]),           // KRW
                msg.tradePrice(),
                Instant.ofEpochMilli(msg.tradeTimestamp()),
                receivedAt
        );
    }
}