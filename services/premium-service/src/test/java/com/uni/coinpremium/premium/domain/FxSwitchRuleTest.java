package com.uni.coinpremium.premium.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FxSwitchRuleTest {

    private final FxSwitchRule rule = new FxSwitchRule(LocalTime.of(11, 0), ZoneId.of("Asia/Seoul"));

    @Test
    @DisplayName("11시 전이면 전날 고시분까지")
    void beforeSwitch() {
        // 11/27 10:30 KST = 01:30 UTC → 11/26 00:00 KST(= 11/25 15:00 UTC) 이하
        assertEquals(Instant.parse("2026-11-25T15:00:00Z"),
                rule.publishedAtLimit(Instant.parse("2026-11-27T01:30:00Z")));
    }

    @Test
    @DisplayName("11시 정각부터는 당일 고시분까지")
    void atSwitch() {
        // 11/27 11:00 KST = 02:00 UTC → 11/27 00:00 KST(= 11/26 15:00 UTC) 이하
        assertEquals(Instant.parse("2026-11-26T15:00:00Z"),
                rule.publishedAtLimit(Instant.parse("2026-11-27T02:00:00Z")));
    }

    @Test
    @DisplayName("KST 자정 직후(UTC로는 전날)도 한국 날짜 기준으로 판단한다")
    void justAfterKstMidnight() {
        // 11/27 00:30 KST = 11/26 15:30 UTC → 11시 전이므로 11/26 00:00 KST 이하
        assertEquals(Instant.parse("2026-11-25T15:00:00Z"),
                rule.publishedAtLimit(Instant.parse("2026-11-26T15:30:00Z")));
    }
}