package com.uni.coinpremium.contract.registry;

import static com.uni.coinpremium.contract.Exchange.BINANCE;
import static com.uni.coinpremium.contract.Exchange.UPBIT;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CoinRegistryTest {

    @Test
    @DisplayName("모든 코인은 김프 계산에 필요한 Upbit·Binance 마켓 코드를 둘 다 가진다")
    void everyCoinHasBothMarkets() {
        assertThat(CoinRegistry.all()).allSatisfy(coin -> {
            assertThat(coin.marketOf(UPBIT)).as(coin.symbol() + " Upbit").isPresent();
            assertThat(coin.marketOf(BINANCE)).as(coin.symbol() + " Binance").isPresent();
        });
    }

    @Test
    @DisplayName("거래소별 구독 목록을 만든다")
    void marketsOf() {
        assertThat(CoinRegistry.marketsOf(UPBIT)).containsExactly("KRW-BTC", "KRW-ETH");
        assertThat(CoinRegistry.marketsOf(BINANCE)).containsExactly("BTCUSDT", "ETHUSDT");
    }

    @Test
    @DisplayName("마켓 코드로 코인을 찾는다")
    void findByMarket() {
        assertThat(CoinRegistry.findByMarket(BINANCE, "ETHUSDT")).map(Coin::symbol).contains("ETH");
        assertThat(CoinRegistry.findByMarket(UPBIT, "KRW-XRP")).isEmpty();   // 지원하지 않는 코인
    }

    @Test
    @DisplayName("심볼로 코인을 찾는다")
    void find() {
        assertThat(CoinRegistry.find("BTC")).map(Coin::name).contains("비트코인");
        assertThat(CoinRegistry.find("DOGE")).isEmpty();
    }
}