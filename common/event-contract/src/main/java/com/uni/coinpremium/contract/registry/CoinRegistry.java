package com.uni.coinpremium.contract.registry;

import static com.uni.coinpremium.contract.Exchange.BINANCE;
import static com.uni.coinpremium.contract.Exchange.UPBIT;
import static java.util.stream.Collectors.toUnmodifiableMap;

import com.uni.coinpremium.contract.Exchange;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * 지원 코인 목록 (서비스 공통 계약)
 * - collector: 거래소별 구독 목록, premium: 김프 계산 대상, API: GET /v1/coins
 * - 코인 추가 = 여기에 한 줄 추가 + 배포 (실행 중 ON/OFF는 Feature Flag가 담당)
 */
public final class CoinRegistry {

    private static final List<Coin> COINS = List.of(
            new Coin("BTC", "비트코인", Map.of(UPBIT, "KRW-BTC", BINANCE, "BTCUSDT")),
            new Coin("ETH", "이더리움", Map.of(UPBIT, "KRW-ETH", BINANCE, "ETHUSDT"))
    );

    // 심볼 중복이 있으면 클래스 로딩 시점에 바로 실패 (실수로 같은 코인을 두 번 넣는 것 방지)
    private static final Map<String, Coin> BY_SYMBOL = COINS.stream()
            .collect(toUnmodifiableMap(Coin::symbol, Function.identity()));

    private CoinRegistry() {
    }

    public static List<Coin> all() {
        return COINS;
    }

    public static Optional<Coin> find(String symbol) {
        return Optional.ofNullable(BY_SYMBOL.get(symbol));
    }

    /** 거래소 구독 목록 (예: UPBIT → [KRW-BTC, KRW-ETH]) */
    public static List<String> marketsOf(Exchange exchange) {
        return COINS.stream()
                .flatMap(coin -> coin.marketOf(exchange).stream())
                .toList();
    }

    /** 거래소 마켓 코드로 코인 찾기 (예: BINANCE, BTCUSDT → BTC) */
    public static Optional<Coin> findByMarket(Exchange exchange, String market) {
        return COINS.stream()
                .filter(coin -> coin.marketOf(exchange).filter(market::equals).isPresent())
                .findFirst();
    }
}