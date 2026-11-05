package com.uni.coinpremium.collector.adapter.in.websocket.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Binance combined stream 메시지: {"stream":"btcusdt@trade","data":{...}}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record BinanceStreamMessage(String stream, Trade data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Trade(
            @JsonProperty("e") String eventType,   // "trade"
            @JsonProperty("s") String symbol,      // "BTCUSDT"
            @JsonProperty("t") long tradeId,       // 체결 ID (심볼별로 유일)
            @JsonProperty("p") BigDecimal price,   // 문자열로 와도 BigDecimal로 정확히 변환됨
            @JsonProperty("T") long tradeTime      // 체결 시각 (ms)
    ) {
        boolean isTrade() {
            return "trade".equals(eventType);
        }
    }
}