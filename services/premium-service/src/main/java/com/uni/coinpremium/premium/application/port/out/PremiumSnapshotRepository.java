package com.uni.coinpremium.premium.application.port.out;

import com.uni.coinpremium.premium.application.service.PremiumSnapshot;
import com.uni.coinpremium.premium.domain.SnapshotSource;

import java.time.Instant;

/**
 * 김프 스냅샷 저장소
 */
public interface PremiumSnapshotRepository {

    /** 저장 성공 시 true, 같은 (symbol, capturedAt)이 이미 있으면 false */
    boolean save(PremiumSnapshot snapshot, Instant capturedAt, SnapshotSource source);
}