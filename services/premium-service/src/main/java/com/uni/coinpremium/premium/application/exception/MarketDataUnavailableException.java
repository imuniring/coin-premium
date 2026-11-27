package com.uni.coinpremium.premium.application.exception;

/**
 * 시세 저장소에 일시적으로 접근할 수 없음 (Valkey 연결 실패·응답 시간 초과)
 * - 데이터가 '없는' 것(MarketDataNotReadyException)과 달리, 있는지조차 확인할 수 없는 상태
 */
public class MarketDataUnavailableException extends RuntimeException {

    public MarketDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}