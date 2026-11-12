package com.uni.coinpremium.premium.application.port.out;

import com.uni.coinpremium.premium.domain.HistoryInterval;
import com.uni.coinpremium.premium.domain.PremiumCandle;

import java.time.Instant;
import java.util.List;

/**
 * 김프 이력 조회 (구간별 캔들)
 */
public interface PremiumHistoryRepository {

    /** [from, to) 범위의 스냅샷을 interval 단위로 묶어 시간순 반환 */
    List<PremiumCandle> findCandles(String symbol, HistoryInterval interval, Instant from, Instant to);
}
