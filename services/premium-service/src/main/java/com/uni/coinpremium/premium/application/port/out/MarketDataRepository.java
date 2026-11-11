package com.uni.coinpremium.premium.application.port.out;

import com.uni.coinpremium.premium.domain.FxRate;
import com.uni.coinpremium.premium.domain.PriceQuote;

import java.util.Optional;

/**
 * 최신 시세·환율 저장소 (구현: 메모리 → 2단계 Redis)
 */
public interface MarketDataRepository {

    /** 기존 값보다 최신일 때만 반영 */
    void saveQuote(PriceQuote quote);

    Optional<PriceQuote> findQuote(String exchange, String symbol);

    /** 기존 값보다 최신일 때만 반영 */
    void saveFxRate(FxRate rate);

    Optional<FxRate> findFxRate();
}