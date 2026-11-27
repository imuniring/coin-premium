package com.uni.coinpremium.premium.adapter.in.web;

import com.uni.coinpremium.contract.Exchange;
import com.uni.coinpremium.contract.registry.Coin;
import java.util.Comparator;
import java.util.List;

/**
 * 코인 1개 응답
 */
public record CoinResponse(String symbol, String name, List<Market> markets) {

    public record Market(Exchange exchange, String market) {
    }

    static CoinResponse from(Coin coin) {
        List<Market> markets = coin.markets().entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().ordinal()))   // 응답 순서 고정 (Map은 순서가 없음)
                .map(e -> new Market(e.getKey(), e.getValue()))
                .toList();
        return new CoinResponse(coin.symbol(), coin.name(), markets);
    }
}
