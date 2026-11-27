package com.uni.coinpremium.premium.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 거래소 REST 주소 (과거 캔들 조회용)
 */
@ConfigurationProperties(prefix = "premium.exchange-api")
public record ExchangeApiProperties(URI upbitUrl, URI binanceUrl, Duration timeout) {
}