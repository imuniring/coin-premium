package com.uni.coinpremium.premium.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 김프 조회를 지원하는 코인 목록
 */
@ConfigurationProperties(prefix = "premium")
public record PremiumProperties(List<String> symbols) {

    public boolean supports(String symbol) {
        return symbols.contains(symbol);
    }
}