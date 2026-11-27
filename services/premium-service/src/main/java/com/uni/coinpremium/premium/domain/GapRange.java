package com.uni.coinpremium.premium.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 스냅샷 공백 구간 (분 단위, 양 끝 포함)
 * 예: from=10:02, to=10:04 → 10:02, 10:03, 10:04 세 분이 비어 있음
 */
public record GapRange(Instant from, Instant to) {

    public GapRange {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (to.isBefore(from)) {
            throw new IllegalStateException("to가 from보다 이름: " + from + " ~ " + to);
        }
    }

    /** 빈 분의 개수 */
    public long minutes() {
        return Duration.between(from, to).toMinutes() + 1;
    }
}
