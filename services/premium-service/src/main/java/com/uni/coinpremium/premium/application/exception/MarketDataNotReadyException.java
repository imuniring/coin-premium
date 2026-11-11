package com.uni.coinpremium.premium.application.exception;

import java.util.List;

/**
 * 김프 계산에 필요한 데이터가 아직 없음 (잠시 후 생김)
 */
public class MarketDataNotReadyException extends RuntimeException {

    private final List<String> missing;

    public MarketDataNotReadyException(String symbol, List<String> missing) {
        super(symbol + " 김프 계산에 필요한 데이터가 아직 준비되지 않았습니다 (missing: " + missing + ")");
        this.missing = List.copyOf(missing);
    }

    public List<String> missing() {
        return missing;
    }
}