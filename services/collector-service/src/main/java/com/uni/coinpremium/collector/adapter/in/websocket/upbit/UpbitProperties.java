package com.uni.coinpremium.collector.adapter.in.websocket.upbit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * Upbit WebSocket 설정 (application.yml의 exchange.upbit)
 */
@ConfigurationProperties(prefix = "exchange.upbit")
public record UpbitProperties(
        URI url
) {}