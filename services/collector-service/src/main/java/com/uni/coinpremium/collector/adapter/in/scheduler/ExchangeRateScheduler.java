package com.uni.coinpremium.collector.adapter.in.scheduler;

import com.uni.coinpremium.collector.application.service.ExchangeRateCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 환율 수집 스케줄러: 기동 5초 후 1회, 이후 이전 실행이 끝나고 30분마다
 */
@Component
public class ExchangeRateScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateScheduler.class);

    private final ExchangeRateCollector collector;

    public ExchangeRateScheduler(ExchangeRateCollector collector) {
        this.collector = collector;
    }

    @Scheduled(initialDelayString = "PT5S", fixedDelayString = "${exchange.koreaexim.poll-interval:PT30M}")
    public void run() {
        // MDC를 바깥에서 열어야 catch의 실패 로그에도 jobId가 남는다
        // (try-with-resources의 catch는 자원을 닫은 뒤에 실행됨)
        try (var job = MDC.putCloseable("job", "exchange-rate");
             var jobId = MDC.putCloseable("jobId", UUID.randomUUID().toString())) {
            collector.collectLatest();
        } catch (Exception e) {
            // 실패해도 스케줄은 계속 돌아야 하므로 여기서 잡고 기록
            log.error("[exchange-rate] 환율 수집 실패", e);
        }
    }
}