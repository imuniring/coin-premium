package com.uni.coinpremium.premium.adapter.out.registry;

import com.uni.coinpremium.contract.registry.Coin;
import com.uni.coinpremium.contract.registry.CoinRegistry;
import com.uni.coinpremium.premium.application.port.out.CoinCatalog;
import com.uni.coinpremium.premium.application.port.out.SupportedCoins;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * 지원 코인 목록을 공통 계약의 CoinRegistry에서 읽는다
 */
@Component
public class CoinRegistryAdapter implements SupportedCoins, CoinCatalog {

    private static final List<String> SYMBOLS = CoinRegistry.all().stream().map(Coin::symbol).toList();

    @Override
    public List<String> symbols() {
        return SYMBOLS;
    }

    @Override
    public List<Coin> all() {
        return CoinRegistry.all();
    }

    @Override
    public Optional<Coin> find(String symbol) {
        return CoinRegistry.find(symbol);
    }
}