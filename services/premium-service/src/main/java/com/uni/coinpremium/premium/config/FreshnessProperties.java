package com.uni.coinpremium.premium.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 시세 신선도 기준
 * @param maxQuoteAge 체결 후 이 시간이 지나면 오래된 시세 (리더 교대 15~20초보다 넉넉하게)
 */
@ConfigurationProperties(prefix = "premium.freshness")
public record FreshnessProperties(Duration maxQuoteAge) {
}