package com.uni.coinpremium.premium.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GapFinderTest {

    private static final Instant FROM = Instant.parse("2026-11-27T10:00:00Z");
    private static final Instant TO = Instant.parse("2026-11-27T10:10:00Z");   // 10:00 ~ 10:09 검사

    @Test
    @DisplayName("모든 분이 있으면 공백이 없다")
    void noGap() {
        assertTrue(GapFinder.find(FROM, TO, minutes(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)).isEmpty());
    }

    @Test
    @DisplayName("연속된 빈 분은 하나의 구간으로 묶는다")
    void groupConsecutive() {
        List<GapRange> gaps = GapFinder.find(FROM, TO, minutes(0, 1, 5, 6, 7, 8, 9));   // 2, 3, 4 비어 있음

        assertEquals(List.of(range(2, 4)), gaps);
        assertEquals(3, gaps.get(0).minutes());
    }

    @Test
    @DisplayName("떨어진 공백은 각각의 구간이 된다")
    void separateGaps() {
        assertEquals(List.of(range(2, 3), range(7, 7)),
                GapFinder.find(FROM, TO, minutes(0, 1, 4, 5, 6, 8, 9)));
    }

    @Test
    @DisplayName("시작과 끝에 걸친 공백도 찾는다")
    void gapsAtEdges() {
        assertEquals(List.of(range(0, 1), range(8, 9)),
                GapFinder.find(FROM, TO, minutes(2, 3, 4, 5, 6, 7)));
    }

    @Test
    @DisplayName("저장된 분이 하나도 없으면 전체가 하나의 구간이다")
    void allMissing() {
        assertEquals(List.of(range(0, 9)), GapFinder.find(FROM, TO, List.of()));
    }

    private static List<Instant> minutes(int... mins) {
        return java.util.Arrays.stream(mins).mapToObj(m -> FROM.plusSeconds(60L * m)).toList();
    }

    private static GapRange range(int fromMin, int toMin) {
        return new GapRange(FROM.plusSeconds(60L * fromMin), FROM.plusSeconds(60L * toMin));
    }
}