package com.uni.coinpremium.premium.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.uni.coinpremium.premium.application.port.out.SnapshotTimeline;
import com.uni.coinpremium.premium.application.port.out.SupportedCoins;
import com.uni.coinpremium.premium.config.BackfillProperties;
import com.uni.coinpremium.premium.domain.GapRange;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GapDetectionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-27T10:30:20Z");
    private static final BackfillProperties PROPS = new BackfillProperties(Duration.ofMinutes(10), Duration.ofMinutes(2));

    @Test
    @DisplayName("최근 2분을 뺀 직전 구간만 검사하고, 공백이 있는 코인만 돌려준다")
    void detectWithinWindow() {
        List<Instant[]> queried = new ArrayList<>();
        SnapshotTimeline timeline = (symbol, from, to) -> {
            queried.add(new Instant[]{from, to});
            // BTC는 10:20만 비어 있고, ETH는 전부 있음
            return minutesBetween(from, to).stream()
                    .filter(m -> !(symbol.equals("BTC") && m.equals(Instant.parse("2026-11-27T10:20:00Z"))))
                    .toList();
        };
        SupportedCoins coins = () -> List.of("BTC", "ETH");
        GapDetectionService service = new GapDetectionService(timeline, coins, PROPS, Clock.fixed(NOW, ZoneOffset.UTC));

        Map<String, List<GapRange>> gaps = service.detect();

        // 검사 구간: 10:30 - 2분 = 10:28 (제외), 그 전 10분 = 10:18 부터
        assertEquals(Instant.parse("2026-11-27T10:18:00Z"), queried.get(0)[0]);
        assertEquals(Instant.parse("2026-11-27T10:28:00Z"), queried.get(0)[1]);
        assertEquals(Map.of("BTC", List.of(new GapRange(
                Instant.parse("2026-11-27T10:20:00Z"), Instant.parse("2026-11-27T10:20:00Z")))), gaps);
    }

    private static List<Instant> minutesBetween(Instant from, Instant to) {
        List<Instant> result = new ArrayList<>();
        for (Instant m = from; m.isBefore(to); m = m.plusSeconds(60)) {
            result.add(m);
        }
        return result;
    }
}