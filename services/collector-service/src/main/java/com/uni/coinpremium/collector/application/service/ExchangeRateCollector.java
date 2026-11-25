package com.uni.coinpremium.collector.application.service;

import com.uni.coinpremium.collector.application.port.out.ExchangeRateFetcher;
import com.uni.coinpremium.collector.application.port.out.ExchangeRatePublisher;
import com.uni.coinpremium.contract.ExchangeRateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 환율 수집 유스케이스
 * 1. 오늘부터 최대 7일 전까지 거꾸로 조회 (주말·연휴 대비)
 * 2. 직전에 발행한 환율과 같으면 발행 생략
 * 재시도·서킷 브레이커는 외부 호출을 담당하는 어댑터(KoreaEximClient)가 처리한다
 */
@Service
public class ExchangeRateCollector {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateCollector.class);
    private static final int LOOKBACK_DAYS = 7;

    private final ExchangeRateFetcher fetcher;
    private final ExchangeRatePublisher publisher;
    private final Clock clock;
    private volatile String lastPublishedEventId;

    public ExchangeRateCollector(ExchangeRateFetcher fetcher, ExchangeRatePublisher publisher, Clock clock) {
        this.fetcher = fetcher;
        this.publisher = publisher;
        this.clock = clock;
    }

    /** 가장 최근 영업일 환율을 찾아 발행 */
    public void collectLatest() {
        LocalDate today = LocalDate.now(clock);   // clock이 KST라 한국 날짜 기준
        for (int i = 0; i < LOOKBACK_DAYS; i++) {
            LocalDate date = today.minusDays(i);
            Optional<ExchangeRateEvent> found = fetcher.fetchUsdKrw(date);
            if (found.isPresent()) {
                publishIfChanged(found.get());
                return;
            }
        }
        log.atWarn()
                .addKeyValue("lookbackDays", LOOKBACK_DAYS)
                .log("[exchange-rate] 최근 {}일간 고시된 환율 없음", LOOKBACK_DAYS);
    }

    private void publishIfChanged(ExchangeRateEvent event) {
        if (event.eventId().equals(lastPublishedEventId)) {
            log.atDebug()
                    .addKeyValue("eventId", event.eventId())
                    .log("[exchange-rate] 변경 없음, 발행 생략");
            return;
        }
        publisher.publish(event);                 // 실패하면 예외 → 아래 줄 실행 안 됨 → 다음 스케줄에 재발행
        lastPublishedEventId = event.eventId();
        log.atInfo()
                .addKeyValue("eventId", event.eventId())
                .addKeyValue("rate", event.rate())
                .log("[exchange-rate] 발행 - rate={}", event.rate());
    }
}