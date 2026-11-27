package com.uni.coinpremium.premium.adapter.in.web;

import static com.uni.coinpremium.contract.Exchange.BINANCE;
import static com.uni.coinpremium.contract.Exchange.UPBIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.uni.coinpremium.contract.registry.Coin;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

class CoinResponseTest {

    @RepeatedTest(20)
    @DisplayName("거래소 순서는 Map 순서와 무관하게 Exchange 정의 순서로 고정된다")
    void marketsInExchangeOrder() {
        Coin btc = new Coin("BTC", "비트코인", Map.of(BINANCE, "BTCUSDT", UPBIT, "KRW-BTC"));

        CoinResponse response = CoinResponse.from(btc);

        assertThat(response.markets()).extracting(CoinResponse.Market::exchange)
                .containsExactly(UPBIT, BINANCE);
    }
}