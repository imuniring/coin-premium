package com.uni.coinpremium.collector.adapter.out.koreaexim;

import com.uni.coinpremium.collector.application.port.out.ExchangeRateFetchException;
import java.util.function.Predicate;

/**
 * Resilience4j 재시도·서킷 브레이커 판단 기준
 * - ExchangeRateFetchException 중 retryable=true(타임아웃, 네트워크, 5xx)만 대상
 * - 서킷 브레이커가 열려 호출이 차단된 경우(CallNotPermittedException) 등 그 외 예외는 대상 아님
 */
public class RetryableFailure implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable e) {
        return e instanceof ExchangeRateFetchException fetch && fetch.isRetryable();
    }
}