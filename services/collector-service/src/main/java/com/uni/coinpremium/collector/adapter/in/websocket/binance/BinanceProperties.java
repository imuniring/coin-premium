package com.uni.coinpremium.collector.adapter.in.websocket.binance;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Binance WebSocket 설정 (application.yml의 exchange.binance)
 */
@ConfigurationProperties(prefix = "exchange.binance")
public record BinanceProperties(
        String url,
        List<String> symbols   // 예: BTCUSDT
) {
    // 예: wss://.../stream?streams=btcusdt@trade/ethusdt@trade
    URI streamUri() {
        String streams = symbols.stream()
                .map(s -> s.toLowerCase() + "@trade")   // Binance 스트림 이름은 소문자
                .collect(Collectors.joining("/"));
        return URI.create(url + "/stream?streams=" + streams);
    }
}