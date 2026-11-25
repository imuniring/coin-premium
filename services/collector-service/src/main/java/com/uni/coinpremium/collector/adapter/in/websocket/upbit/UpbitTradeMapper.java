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
                // sequential_id의 유일성 범위(전체/마켓별)가 문서에 없어 마켓 코드를 포함한다 (ADR 0008)
                // 마켓별 번호라면 같은 밀리초에 체결된 다른 코인과 ID가 겹칠 수 있음
                "UPBIT-" + msg.code() + "-" + msg.sequentialId(),   // 예: UPBIT-KRW-BTC-17938843438150000
                Exchange.UPBIT,
                parts[1],                                  // BTC
                QuoteCurrency.valueOf(parts[0]),           // KRW
                msg.tradePrice(),
                Instant.ofEpochMilli(msg.tradeTimestamp()),
                receivedAt
        );
    }
}