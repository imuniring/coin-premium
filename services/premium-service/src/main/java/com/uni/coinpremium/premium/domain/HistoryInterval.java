package com.uni.coinpremium.premium.domain;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * 김프 이력 조회 간격과 간격별 최대 조회 기간
 */
public enum HistoryInterval {

    MINUTE("1m", Duration.ofDays(1)),   // 최대 1,440개
    HOUR("1h", Duration.ofDays(31)),    // 최대 744개
    DAY("1d", Duration.ofDays(366));    // 최대 366개 (KST 자정 기준)

    private final String code;
    private final Duration maxRange;

    HistoryInterval(String code, Duration maxRange) {
        this.code = code;
        this.maxRange = maxRange;
    }

    public String code() {
        return code;
    }

    public Duration maxRange() {
        return maxRange;
    }

    // "1h" → HOUR, 모르는 값이면 empty
    public static Optional<HistoryInterval> fromCode(String code) {
        return Arrays.stream(values())
                .filter(i -> i.code.equals(code))
                .findFirst();
    }
}
