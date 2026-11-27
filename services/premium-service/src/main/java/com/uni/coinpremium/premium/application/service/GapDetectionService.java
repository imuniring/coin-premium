package com.uni.coinpremium.premium.application.service;

import com.uni.coinpremium.premium.application.port.out.SnapshotTimeline;
import com.uni.coinpremium.premium.application.port.out.SupportedCoins;
import com.uni.coinpremium.premium.config.BackfillProperties;
import com.uni.coinpremium.premium.domain.GapFinder;
import com.uni.coinpremium.premium.domain.GapRange;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 최근 구간의 스냅샷 공백 찾기 (코인별)
 */
@Service
public class GapDetectionService {

    private final SnapshotTimeline timeline;
    private final SupportedCoins supportedCoins;
    private final BackfillProperties properties;
    private final Clock clock;

    public GapDetectionService(SnapshotTimeline timeline, SupportedCoins supportedCoins,
                               BackfillProperties properties, Clock clock) {
        this.timeline = timeline;
        this.supportedCoins = supportedCoins;
        this.properties = properties;
        this.clock = clock;
    }

    /** 코인 → 공백 구간 목록 (공백이 없는 코인은 빠짐) */
    public Map<String, List<GapRange>> detect() {
        Instant now = clock.instant().truncatedTo(ChronoUnit.MINUTES);
        Instant to = now.minus(properties.settleDelay());   // 최근 2분 제외
        Instant from = to.minus(properties.lookback());      // 그 전 6시간

        Map<String, List<GapRange>> result = new LinkedHashMap<>();   // 코인 순서 유지 (로그 읽기 쉽게)
        for (String symbol : supportedCoins.symbols()) {
            List<GapRange> gaps = GapFinder.find(from, to, timeline.capturedMinutes(symbol, from, to));
            if (!gaps.isEmpty()) {
                result.put(symbol, gaps);
            }
        }
        return result;
    }
}