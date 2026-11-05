package com.uni.coinpremium.collector.adapter.in.websocket.upbit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Upbit 체결(trade) 메시지 원본
 */
@JsonIgnoreProperties(ignoreUnknown = true)   // 안 쓰는 필드는 무시
record UpbitTradeMessage(
        String type,                                         // "trade"
        String code,                                         // "KRW-BTC"
        @JsonProperty("trade_price") BigDecimal tradePrice,  // BigDecimal로 바로 읽음 (오차 없음)
        @JsonProperty("trade_timestamp") long tradeTimestamp,// 체결 시각 (ms)
        @JsonProperty("sequential_id") long sequentialId     // 체결 고유 번호
) {
    boolean isTrade() {
        return "trade".equals(type);
    }
}