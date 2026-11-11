package com.uni.coinpremium.premium.application.service;

import com.uni.coinpremium.premium.application.exception.MarketDataNotReadyException;
import com.uni.coinpremium.premium.application.port.out.PremiumSnapshotRepository;
import com.uni.coinpremium.premium.config.PremiumProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 김프 스냅샷 저장
 * - 모든 코인을 같은 기준 시각(분 단위)으로 저장
 * - 코인 하나가 실패해도 나머지 코인은 계속 저장
 */
@Service
public class PremiumSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(PremiumSnapshotService.class);

    private final PremiumQueryService queryService;
    private final PremiumSnapshotRepository repository;
    private final PremiumProperties properties;
    private final Clock clock;

    public PremiumSnapshotService(PremiumQueryService queryService, PremiumSnapshotRepository repository,
                                  PremiumProperties properties, Clock clock) {
        this.queryService = queryService;
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    public void captureAll() {
        // 05:01:37 → 05:01:00 (스케줄러가 몇 초 늦게 돌아도 같은 "분"으로 묶임)
        Instant capturedAt = clock.instant().truncatedTo(ChronoUnit.MINUTES);
        for (String symbol : properties.symbols()) {
            capture(symbol, capturedAt);
        }
    }

    private void capture(String symbol, Instant capturedAt) {
        try {
            // 계산은 조회 API와 같은 서비스 재사용 → API 값과 저장 값이 항상 같은 규칙
            boolean saved = repository.save(queryService.getPremium(symbol), capturedAt);
            if (saved) {
                log.info("[snapshot] 저장 - symbol={}, capturedAt={}", symbol, capturedAt);
            } else {
                log.debug("[snapshot] 이미 저장된 시점 - symbol={}, capturedAt={}", symbol, capturedAt);
            }
        } catch (MarketDataNotReadyException e) {
            // 기동 직후엔 정상 상황
            log.info("[snapshot] 데이터 준비 전, 건너뜀 - symbol={}, missing={}", symbol, e.missing());
        } catch (Exception e) {
            // 한 코인의 실패가 다른 코인 저장을 막지 않도록 여기서 잡음
            log.error("[snapshot] 저장 실패 - symbol={}", symbol, e);
        }
    }
}