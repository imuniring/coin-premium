package com.uni.coinpremium.premium.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * 1분 캔들의 종가
 * @param minuteStart 캔들 시작 시각 (10:01:00이면 10:01:00~10:01:59 구간)
 * @param close       그 구간 마지막 체결 가격
 */
public record MinuteClose(Instant minuteStart, BigDecimal close) {

    public MinuteClose {
        Objects.requireNonNull(minuteStart, "minuteStart");
        Objects.requireNonNull(close, "close");
    }
}