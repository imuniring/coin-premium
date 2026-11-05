package com.uni.coinpremium.collector.application.port.out;

/**
 * 환율 조회 실패
 * - retryable=true: 일시적 실패 (타임아웃, 네트워크, 5xx) → 재시도
 * - retryable=false: 영구 실패 (인증 오류, 일일 제한 초과 등) → 재시도 안 함
 */
public class ExchangeRateFetchException extends RuntimeException {

    private final boolean retryable;

    public ExchangeRateFetchException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public ExchangeRateFetchException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}