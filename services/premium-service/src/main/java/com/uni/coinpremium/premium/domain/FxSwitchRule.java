package com.uni.coinpremium.premium.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 환율 일자 전환 규칙
 * - 수출입은행 환율은 일환율로 영업일 11시 전후 업데이트 → 고시일 D의 환율은 D switchTime(KST)부터 적용
 * - 그 전에는 전날까지 고시된 환율 중 최신 (주말·공휴일은 자연히 직전 영업일)
 * - 고시 기준 시각(publishedAt)은 collector 규칙상 '고시일 00:00 KST'
 */
public record FxSwitchRule(LocalTime switchTime, ZoneId zone) {

    /** t 시각에 적용할 수 있는 환율의 publishedAt 상한 (이 시각 이하에서 가장 최근 환율을 쓴다) */
    public Instant publishedAtLimit(Instant t) {
        ZonedDateTime local = t.atZone(zone);
        LocalDate effectiveDate = local.toLocalTime().isBefore(switchTime)
                ? local.toLocalDate().minusDays(1)   // 전환 시각 전 → 전날 고시분까지
                : local.toLocalDate();               // 전환 시각 이후 → 당일 고시분까지
        return effectiveDate.atStartOfDay(zone).toInstant();
    }
}