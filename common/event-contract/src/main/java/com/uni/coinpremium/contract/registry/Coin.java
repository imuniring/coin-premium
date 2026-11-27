package com.uni.coinpremium.contract.registry;

import com.uni.coinpremium.contract.Exchange;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 지원 코인 1개
 * @param symbol  서비스 공통 심볼 (예: BTC)
 * @param name    표시 이름 (예: 비트코인)
 * @param markets 거래소별 마켓 코드 (예: UPBIT → KRW-BTC, BINANCE → BTCUSDT)
 */
public record Coin(String symbol, String name, Map<Exchange, String> markets) {

    public Coin {
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(name, "name");
        markets = Map.copyOf(markets);   // 밖에서 넘긴 Map을 나중에 바꿔도 영향 없게 복사 (불변)
        if (markets.isEmpty()) {
            throw new IllegalArgumentException("마켓 코드가 하나도 없음: " + symbol);
        }
    }

    /** 이 거래소에서의 마켓 코드 (해당 거래소에 상장되지 않았으면 비어 있음) */
    public Optional<String> marketOf(Exchange exchange) {
        return Optional.ofNullable(markets.get(exchange));
    }
}