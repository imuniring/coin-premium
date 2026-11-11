package com.uni.coinpremium.premium.application.port.out;

import com.uni.coinpremium.premium.application.service.PremiumSnapshot;

import java.time.Instant;

/**
 * 김프 스냅샷 저장소
 */
public interface PremiumSnapshotRepository {

    /** 저장했으면 true, 같은 코인·같은 시점 스냅샷이 이미 있으면 false */
    boolean save(PremiumSnapshot snapshot, Instant capturedAt);
}