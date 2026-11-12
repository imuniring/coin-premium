package com.uni.coinpremium.premium.application.exception;

/**
 * 이력 조회 기간이 잘못됨 (from ≥ to, 최대 기간 초과)
 */
public class InvalidHistoryRangeException extends RuntimeException {
    public InvalidHistoryRangeException(String message) {
        super(message);
    }
}