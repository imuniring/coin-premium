package com.uni.coinpremium.premium.application.port.out;

import com.uni.coinpremium.contract.registry.Coin;

import java.util.List;
import java.util.Optional;

/**
 * 지원 코인의 상세 정보 (이름, 거래소별 마켓 코드)
 */
public interface CoinCatalog {

    List<Coin> all();

    Optional<Coin> find(String symbol);
}
