package com.uni.coinpremium.premium.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공백 감지·보정 설정
 * @param lookback    검사할 과거 범위 (이보다 오래된 공백은 일일 배치 몫)
 * @param settleDelay 최근 이만큼은 검사에서 제외 (실시간 저장과 경쟁하지 않게)
 */
@ConfigurationProperties(prefix = "premium.backfill")
public record BackfillProperties(Duration lookback, Duration settleDelay) {
}