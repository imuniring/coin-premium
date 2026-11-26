package com.uni.coinpremium.premium.adapter.in.scheduler;

import com.uni.coinpremium.premium.application.service.PremiumSnapshotService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

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
    @SchedulerLock(
            name = "premium-snapshot",
            lockAtMostFor = "50s",    // 실행 중 죽어도 다음 분(1분 주기)엔 다른 인스턴스가 실행 가능
            lockAtLeastFor = "30s"    // 일이 0.05초 만에 끝나도 30초 보유 → 늦게 깨어난 인스턴스의 중복 실행 방지
    )
    public void run() {
        // 실행 1회를 jobId로 묶어 코인별 저장 로그를 한 번에 찾을 수 있게 한다
        try (var job = MDC.putCloseable("job", "premium-snapshot");
             var jobId = MDC.putCloseable("jobId", UUID.randomUUID().toString())) {
            snapshotService.captureAll();   // 예외는 서비스에서 코인별로 처리
        }
    }
}