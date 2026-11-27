package com.uni.coinpremium.premium.application.service;

import com.uni.coinpremium.premium.domain.FxRate;
import com.uni.coinpremium.premium.domain.KimchiPremium;
import com.uni.coinpremium.premium.domain.PriceQuote;

/**
 * 김프 + 계산에 사용한 원본 값 (언제 기준의 값인지 함께 보여주기 위함)
 */
public record PremiumSnapshot(
        KimchiPremium premium,
        PriceQuote domestic,
        PriceQuote foreign,
        FxRate fxRate,
        boolean stale
) {
    /** 같은 내용을 오래된 값으로 표시 (저장소 장애로 최신 여부를 확인할 수 없을 때) */
    public PremiumSnapshot asStale() {
        return new PremiumSnapshot(premium, domestic, foreign, fxRate, true);
    }
}