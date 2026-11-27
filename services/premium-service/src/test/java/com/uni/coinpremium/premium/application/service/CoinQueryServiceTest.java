package com.uni.coinpremium.premium.application.service;

import static com.uni.coinpremium.contract.Exchange.BINANCE;
import static com.uni.coinpremium.contract.Exchange.UPBIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.uni.coinpremium.contract.registry.Coin;
import com.uni.coinpremium.premium.application.exception.UnsupportedSymbolException;
import com.uni.coinpremium.premium.application.port.out.CoinCatalog;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CoinQueryServiceTest {

    private static final Coin BTC = new Coin("BTC", "비트코인", Map.of(UPBIT, "KRW-BTC", BINANCE, "BTCUSDT"));

    // 테스트용 카탈로그: BTC 하나만 있음
    private final CoinCatalog catalog = new CoinCatalog() {
        @Override
        public List<Coin> all() {
            return List.of(BTC);
        }

        @Override
        public Optional<Coin> find(String symbol) {
            return "BTC".equals(symbol) ? Optional.of(BTC) : Optional.empty();
        }
    };
    private final CoinQueryService service = new CoinQueryService(catalog);

    @Test
    @DisplayName("지원 코인 목록을 돌려준다")
    void getCoins() {
        assertThat(service.getCoins()).containsExactly(BTC);
    }

    @Test
    @DisplayName("지원하지 않는 코인이면 UnsupportedSymbolException (김프 조회와 같은 404)")
    void unsupported() {
        assertThatThrownBy(() -> service.getCoin("XRP")).isInstanceOf(UnsupportedSymbolException.class);
    }
}