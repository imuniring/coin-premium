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
        String url
) {}