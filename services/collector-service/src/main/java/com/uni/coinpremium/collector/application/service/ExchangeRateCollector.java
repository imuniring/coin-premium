package com.uni.coinpremium.collector.application.service;

import com.uni.coinpremium.collector.application.port.out.ExchangeRateFetchException;
import com.uni.coinpremium.collector.application.port.out.ExchangeRateFetcher;
import com.uni.coinpremium.collector.application.port.out.ExchangeRatePublisher;
import com.uni.coinpremium.contract.ExchangeRateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 환율 수집 유스케이스
 * 1. 오늘부터 최대 7일 전까지 거꾸로 조회 (주말·연휴 대비)
 * 2. 일시적 실패는 짧게 재시도 (1s → 2s, 최대 3회), 영구 실패는 즉시 중단
 * 3. 직전에 발행한 환율과 같으면 발행 생략
 */
@Service
public class ExchangeRateCollector {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateCollector.class);
    private static final int LOOKBACK_DAYS = 7;
    private static final int MAX_ATTEMPTS = 3;

    private final ExchangeRateFetcher fetcher;
    private final ExchangeRatePublisher publisher;
    private final Clock clock;
    private final Duration retryBackoff;
    private volatile String lastPublishedEventId;

    @Autowired   // 생성자가 두 개라 Spring이 쓸 생성자를 지정
    public ExchangeRateCollector(ExchangeRateFetcher fetcher, ExchangeRatePublisher publisher, Clock clock) {
        this(fetcher, publisher, clock, Duration.ofSeconds(1));
    }

    // 테스트에서 재시도 대기 시간을 0으로 주입하기 위한 생성자
    ExchangeRateCollector(ExchangeRateFetcher fetcher, ExchangeRatePublisher publisher,
                          Clock clock, Duration retryBackoff) {
        this.fetcher = fetcher;
        this.publisher = publisher;
        this.clock = clock;
        this.retryBackoff = retryBackoff;
    }

    /** 가장 최근 영업일 환율을 찾아 발행 */
    public void collectLatest() {
        LocalDate today = LocalDate.now(clock);   // clock이 KST라 한국 날짜 기준
        for (int i = 0; i < LOOKBACK_DAYS; i++) {
            LocalDate date = today.minusDays(i);
            Optional<ExchangeRateEvent> found = fetchWithRetry(date);
            if (found.isPresent()) {
                publishIfChanged(found.get());
                return;
            }
        }
        log.warn("[exchange-rate] 최근 {}일간 고시된 환율 없음", LOOKBACK_DAYS);
    }

    private Optional<ExchangeRateEvent> fetchWithRetry(LocalDate date) {
        for (int attempt = 1; ; attempt++) {
            try {
                return fetcher.fetchUsdKrw(date);
            } catch (ExchangeRateFetchException e) {
                // 영구 실패이거나 재시도를 다 썼으면 포기
                if (!e.isRetryable() || attempt >= MAX_ATTEMPTS) {
                    throw e;
                }
                long waitMs = retryBackoff.toMillis() << (attempt - 1);   // 1s, 2s
                log.warn("[exchange-rate] 일시적 실패 ({}회차), {}ms 후 재시도: {}", attempt, waitMs, e.getMessage());
                sleep(waitMs);
            }
        }
    }

    private void publishIfChanged(ExchangeRateEvent event) {
        if (event.eventId().equals(lastPublishedEventId)) {
            log.debug("[exchange-rate] 변경 없음, 발행 생략 - {}", event.eventId());
            return;
        }
        publisher.publish(event);                 // 실패하면 예외 → 아래 줄 실행 안 됨 → 다음 스케줄에 재발행
        lastPublishedEventId = event.eventId();
        log.info("[exchange-rate] 발행 - rate={}, 기준일={}", event.rate(), event.eventId());
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExchangeRateFetchException("retry interrupted", e, false);
        }
    }
}