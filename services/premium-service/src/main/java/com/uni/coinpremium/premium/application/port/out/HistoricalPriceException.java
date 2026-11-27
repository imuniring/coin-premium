package com.uni.coinpremium.premium.application.port.out;

/**
 * 거래소 과거 시세 조회 실패
 */
public class HistoricalPriceException extends RuntimeException {

    public HistoricalPriceException(String message) {
        super(message);
    }

    public HistoricalPriceException(String message, Throwable cause) {
        super(message, cause);
    }
}