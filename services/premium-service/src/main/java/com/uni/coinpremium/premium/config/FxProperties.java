package com.uni.coinpremium.premium.config;

import java.time.LocalTime;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 환율 적용 설정
 * @param switchTime 고시일 환율 적용 시작 시각 (KST). 수출입은행 안내: 영업일 11시 전후 업데이트
 */
@ConfigurationProperties(prefix = "premium.fx")
public record FxProperties(LocalTime switchTime) {
}