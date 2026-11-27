package com.uni.coinpremium.premium.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 스냅샷 공백 찾기 (순수 계산)
 * 있어야 할 분 [from, to) 중 저장되지 않은 분을 연속 구간으로 묶는다
 */
public final class GapFinder {

    private GapFinder() {
    }

    /**
     * @param from     검사 시작 (포함, 분 단위로 내림)
     * @param to       검사 끝 (제외, 분 단위로 내림)
     * @param captured 실제로 저장된 분들
     */
    public static List<GapRange> find(Instant from, Instant to, Collection<Instant> captured) {
        Set<Instant> existing = new HashSet<>(captured);   // 매번 목록을 훑지 않도록 Set으로 (포함 여부 확인이 빠름)
        List<GapRange> gaps = new ArrayList<>();

        Instant gapStart = null;   // 지금 이어지고 있는 공백의 시작 (없으면 null)
        Instant end = to.truncatedTo(ChronoUnit.MINUTES);
        for (Instant m = from.truncatedTo(ChronoUnit.MINUTES); m.isBefore(end); m = m.plus(1, ChronoUnit.MINUTES)) {
            boolean missing = !existing.contains(m);
            if (missing && gapStart == null) {
                gapStart = m;                                                 // 공백 시작
            } else if (!missing && gapStart != null) {
                gaps.add(new GapRange(gapStart, m.minus(1, ChronoUnit.MINUTES)));   // 공백 끝 (직전 분까지)
                gapStart = null;
            }
        }
        if (gapStart != null) {
            gaps.add(new GapRange(gapStart, end.minus(1, ChronoUnit.MINUTES)));    // 검사 끝까지 비어 있던 경우
        }
        return gaps;
    }
}