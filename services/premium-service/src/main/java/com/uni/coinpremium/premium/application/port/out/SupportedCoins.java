package com.uni.coinpremium.premium.application.port.out;

import java.util.List;

/**
 * 김프 조회·저장을 지원하는 코인 목록
 */
public interface SupportedCoins {

    List<String> symbols();

    default boolean supports(String symbol) {
        return symbols().contains(symbol);
    }
}