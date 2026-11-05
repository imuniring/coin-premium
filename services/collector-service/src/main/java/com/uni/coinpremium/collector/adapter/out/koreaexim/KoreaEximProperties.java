package com.uni.coinpremium.collector.adapter.out.koreaexim;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 수출입은행 환율 API 설정
 */
@ConfigurationProperties(prefix = "exchange.koreaexim")
public record KoreaEximProperties(String url, String apiKey) {

    // record의 기본 toString은 모든 필드를 출력함 → 키가 로그에 찍히지 않게 덮어씀
    @Override
    public String toString() {
        return "KoreaEximProperties[url=" + url + ", apiKey=****]";
    }
}