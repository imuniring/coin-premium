package com.uni.coinpremium.collector.adapter.out.koreaexim;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 환율 API 재시도·서킷 브레이커 이벤트를 로그로 남긴다
 * - 재시도: 직접 구현 시절의 "일시적 실패 (n회차)" 로그 유지
 * - 서킷 상태 변경: CLOSED → OPEN → HALF_OPEN 전환을 기록 (Actuator 도입 전 유일한 상태 확인 수단)
 * 이벤트는 호출한 스레드(스케줄러)에서 실행되어 job, jobId MDC가 그대로 붙는다
 */
@Component
class KoreaEximResilienceLogger {

    private static final Logger log = LoggerFactory.getLogger(KoreaEximResilienceLogger.class);
    private static final String NAME = "koreaexim";   // yml의 instances 이름, @Retry·@CircuitBreaker name과 동일

    KoreaEximResilienceLogger(RetryRegistry retryRegistry, CircuitBreakerRegistry circuitBreakerRegistry) {
        retryRegistry.retry(NAME).getEventPublisher()
                .onRetry(e -> {
                    long delayMs = e.getWaitInterval().toMillis();
                    log.atWarn()
                            .addKeyValue("attempt", e.getNumberOfRetryAttempts())
                            .addKeyValue("delayMs", delayMs)
                            .log("[exchange-rate] 일시적 실패 ({}회차), {}ms 후 재시도: {}",
                                    e.getNumberOfRetryAttempts(), delayMs, e.getLastThrowable().getMessage());
                });

        circuitBreakerRegistry.circuitBreaker(NAME).getEventPublisher()
                .onStateTransition(e -> {
                    String from = e.getStateTransition().getFromState().name();
                    String to = e.getStateTransition().getToState().name();
                    log.atWarn()
                            .addKeyValue("fromState", from)
                            .addKeyValue("toState", to)
                            .log("[exchange-rate] 서킷 상태 변경 {} → {}", from, to);
                });
    }
}