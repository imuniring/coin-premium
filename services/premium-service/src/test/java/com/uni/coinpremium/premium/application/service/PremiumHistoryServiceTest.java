package com.uni.coinpremium.premium.application.service;

import com.uni.coinpremium.premium.application.exception.InvalidHistoryRangeException;
import com.uni.coinpremium.premium.config.PremiumProperties;
import com.uni.coinpremium.premium.domain.HistoryInterval;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PremiumHistoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T06:00:00Z");

    // 저장소는 빈 목록을 돌려주는 람다 (기간 계산·검증만 확인)
    private final PremiumHistoryService service = new PremiumHistoryService(
            (symbol, interval, from, to) -> List.of(),
            new PremiumProperties(List.of("BTC")),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("from·to가 없으면 최근 1일을 조회한다")
    void defaultRange() {
        PremiumHistory h = service.getHistory("BTC", HistoryInterval.HOUR, null, null);

        assertEquals(Instant.parse("2026-11-10T06:00:00Z"), h.from());
        assertEquals(NOW, h.to());
    }

    @Test
    @DisplayName("from이 to보다 같거나 늦으면 예외")
    void fromAfterTo() {
        assertThrows(InvalidHistoryRangeException.class, () ->
                service.getHistory("BTC", HistoryInterval.HOUR, NOW, NOW));
    }

    @Test
    @DisplayName("간격별 최대 기간(1m은 1일)을 넘으면 예외")
    void exceedMaxRange() {
        Instant from = NOW.minusSeconds(60 * 60 * 25);   // 25시간 전
        assertThrows(InvalidHistoryRangeException.class, () ->
                service.getHistory("BTC", HistoryInterval.MINUTE, from, NOW));
    }
}