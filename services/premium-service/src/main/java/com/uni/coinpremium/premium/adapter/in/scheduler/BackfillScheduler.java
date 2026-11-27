package com.uni.coinpremium.premium.adapter.in.scheduler;

import com.uni.coinpremium.premium.application.service.BackfillResult;
import com.uni.coinpremium.premium.application.service.BackfillService;
import java.util.UUID;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 공백 보정 스케줄러: 10분마다 최근 구간의 빈 스냅샷을 채움
 * - cron으로 모든 인스턴스의 실행 시각을 맞춰 ShedLock으로 한 대만 실행
 * - 매분 0초 스냅샷과 겹치지 않게 30초에 실행
 */
@Component
public class BackfillScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackfillScheduler.class);

    private final BackfillService backfillService;

    public BackfillScheduler(BackfillService backfillService) {
        this.backfillService = backfillService;
    }

    @Scheduled(cron = "${premium.backfill.cron:30 */10 * * * *}")
    @SchedulerLock(
            name = "premium-backfill",
            lockAtMostFor = "9m",    // 주기(10분)보다 짧게: 실행 중 죽어도 다음 회차엔 다른 인스턴스가 실행
            lockAtLeastFor = "1m"    // 인스턴스 간 시작 시각 차이 흡수
    )
    public void run() {
        // catch 로그에도 jobId가 남도록 MDC를 바깥에서 연다 (트러블슈팅 015)
        try (var job = MDC.putCloseable("job", "premium-backfill");
             var jobId = MDC.putCloseable("jobId", UUID.randomUUID().toString())) {
            try {
                BackfillResult result = backfillService.backfillRecent();
                logResult(result);
            } catch (Exception e) {
                // 실패해도 다음 회차는 돌아야 하므로 여기서 잡고 기록
                log.error("[backfill] 공백 보정 실패", e);
            }
        }
    }

    private static void logResult(BackfillResult r) {
        if (r.equals(BackfillResult.EMPTY)) {
            log.debug("[backfill] 공백 없음");
            return;
        }
        log.atInfo()
                .addKeyValue("filled", r.filled())
                .addKeyValue("unfillable", r.unfillable())
                .addKeyValue("failed", r.failed())
                .log("[backfill] 완료 - 채움 {}, 못 채움 {}, 실패 {}", r.filled(), r.unfillable(), r.failed());
    }
}