package com.uni.coinpremium.collector.adapter.in.websocket.upbit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.List;

/**
 * Upbit WebSocket 설정 (application.yml의 exchange.upbit)
 */
@ConfigurationProperties(prefix = "exchange.upbit")
public record UpbitProperties(
        URI url,
        List<String> codes   // 구독할 마켓 코드 (예: KRW-BTC)
) {}