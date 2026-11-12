package com.uni.coinpremium.premium.application.service;

import com.uni.coinpremium.premium.application.exception.InvalidHistoryRangeException;
import com.uni.coinpremium.premium.application.exception.UnsupportedSymbolException;
import com.uni.coinpremium.premium.application.port.out.PremiumHistoryRepository;
import com.uni.coinpremium.premium.config.PremiumProperties;
import com.uni.coinpremium.premium.domain.HistoryInterval;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * 김프 이력 조회
 * - from·to를 안 주면 최근 1일
 * - from < to, 간격별 최대 기간 이내만 허용
 */
@Service
public class PremiumHistoryService {

    private static final Duration DEFAULT_RANGE = Duration.ofDays(1);   // 모든 간격의 최대 기간 이내

    private final PremiumHistoryRepository repository;
    private final PremiumProperties properties;
    private final Clock clock;

    public PremiumHistoryService(PremiumHistoryRepository repository, PremiumProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    public PremiumHistory getHistory(String symbol, HistoryInterval interval, Instant from, Instant to) {
        if (!properties.supports(symbol)) {
            throw new UnsupportedSymbolException(symbol);
        }

        Instant end = (to != null) ? to : clock.instant();
        Instant start = (from != null) ? from : end.minus(DEFAULT_RANGE);

        if (!start.isBefore(end)) {
            throw new InvalidHistoryRangeException("from은 to보다 이전이어야 합니다.");
        }
        if (Duration.between(start, end).compareTo(interval.maxRange()) > 0) {
            throw new InvalidHistoryRangeException(
                    interval.code() + " 간격은 최대 " + interval.maxRange().toDays() + "일까지 조회할 수 있습니다.");
        }

        return new PremiumHistory(symbol, interval, start, end,
                repository.findCandles(symbol, interval, start, end));
    }
}