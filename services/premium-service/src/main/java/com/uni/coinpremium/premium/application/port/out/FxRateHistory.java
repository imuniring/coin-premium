package com.uni.coinpremium.premium.application.port.out;

import com.uni.coinpremium.premium.domain.FxRate;
import java.time.Instant;
import java.util.Optional;

/**
 * 환율 이력
 */
public interface FxRateHistory {

    /** 저장 (같은 고시 시각이 이미 있으면 무시) */
    void save(FxRate rate, String eventId);

    /** publishedAt이 limit 이하인 환율 중 가장 최근 */
    Optional<FxRate> findLatestPublishedUpTo(Instant limit);
}