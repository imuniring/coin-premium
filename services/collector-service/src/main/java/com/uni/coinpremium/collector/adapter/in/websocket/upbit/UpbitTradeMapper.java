package com.uni.coinpremium.collector.adapter.in.websocket.upbit;

import com.uni.coinpremium.contract.Exchange;
import com.uni.coinpremium.contract.QuoteCurrency;
import com.uni.coinpremium.contract.TickerEvent;
import com.uni.coinpremium.contract.registry.Coin;
import com.uni.coinpremium.contract.registry.CoinRegistry;

import java.time.Instant;

/**
 * Upbit 체결 메시지 → TickerEvent 변환
 */
final class UpbitTradeMapper {

    private UpbitTradeMapper() {}

    static TickerEvent toEvent(UpbitTradeMessage msg, Instant receivedAt) {
        // "KRW-BTC" → 레지스트리에서 BTC를 찾음 (지원하지 않는 마켓이면 예외)
        Coin coin = CoinRegistry.findByMarket(Exchange.UPBIT, msg.code())
                .orElseThrow(() -> new IllegalArgumentException("unsupported market: " + msg.code()));
        // 호가 통화는 마켓 코드 앞부분 (KRW-BTC → KRW)
        QuoteCurrency quote = QuoteCurrency.valueOf(msg.code().substring(0, msg.code().indexOf('-')));

        return new TickerEvent(
                // sequential_id의 유일성 범위(전체/마켓별)가 문서에 없어 마켓 코드를 포함한다 (ADR 0008)
                "UPBIT-" + msg.code() + "-" + msg.sequentialId(),
                Exchange.UPBIT,
                coin.symbol(),                             // BTC
                quote,                                     // KRW
                msg.tradePrice(),
                Instant.ofEpochMilli(msg.tradeTimestamp()),
                receivedAt
        );
    }
}