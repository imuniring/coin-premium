package com.uni.coinpremium.premium.adapter.in.scheduler;

import com.uni.coinpremium.premium.application.service.PremiumSnapshotService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매 분 0초에 김프 스냅샷 저장
 */
@Component
public class PremiumSnapshotScheduler {

    private final PremiumSnapshotService snapshotService;

    public PremiumSnapshotScheduler(PremiumSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @Scheduled(cron = "0 * * * * *")   // 초 분 시 일 월 요일 → "매 분 0초"
    public void run() {
        snapshotService.captureAll();   // 예외는 서비스에서 코인별로 처리
    }
}