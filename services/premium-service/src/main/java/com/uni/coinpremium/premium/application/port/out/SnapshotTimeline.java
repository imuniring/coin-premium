package com.uni.coinpremium.premium.application.port.out;

import java.time.Instant;
import java.util.List;

/**
 * 저장된 스냅샷 시점 조회 (공백 감지용)
 */
public interface SnapshotTimeline {

    /** [from, to) 구간에 저장된 captured_at 목록 */
    List<Instant> capturedMinutes(String symbol, Instant from, Instant to);
}