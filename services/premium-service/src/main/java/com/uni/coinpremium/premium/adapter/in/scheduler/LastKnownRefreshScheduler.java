package com.uni.coinpremium.premium.adapter.in.scheduler;

import com.uni.coinpremium.premium.application.port.out.SupportedCoins;
import com.uni.coinpremium.premium.application.service.PremiumQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 인스턴스마다 마지막 김프를 갱신 (Valkey 장애 시 stale 응답용)
 * - 스냅샷 저장은 ShedLock으로 한 대만 하지만, 마지막 값 기억은 인스턴스 메모리라 모든 인스턴스가 각자 갱신해야 함
 * - 그래서 ShedLock 없음
 */
@Component
public class LastKnownRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(LastKnownRefreshScheduler.class);

    private final PremiumQueryService queryService;
    private final SupportedCoins supportedCoins;

    public LastKnownRefreshScheduler(PremiumQueryService queryService, SupportedCoins supportedCoins) {
        this.queryService = queryService;
        this.supportedCoins = supportedCoins;
    }

    @Scheduled(
            initialDelayString = "${premium.last-known.initial-delay:PT5S}",     // 기동 직후 기억이 빈 구간을 짧게
            fixedDelayString = "${premium.last-known.refresh-interval:PT30S}"
    )
    public void refresh() {
        for (String symbol : supportedCoins.symbols()) {
            try {
                queryService.getPremium(symbol);   // 성공하면 그 안에서 마지막 값이 갱신됨
            } catch (Exception e) {
                // 데이터 준비 전·저장소 장애는 다음 회차에 다시 (장애 자체는 조회·스냅샷 로그로 보임)
                log.debug("[last-known] 갱신 건너뜀 - symbol={}, reason={}", symbol, e.getClass().getSimpleName());
            }
        }
    }
}