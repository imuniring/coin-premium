package com.uni.coinpremium.premium.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 김프 캔들 (1분 스냅샷을 구간별로 묶은 값)
 *
 * @param time  구간 시작 시각 (UTC)
 * @param count 구간에 포함된 스냅샷 수 (1h면 60이 정상, 적으면 공백 존재)
 */
public record PremiumCandle(
        Instant time,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal avg,
        int count
) {}