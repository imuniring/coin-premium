package com.uni.coinpremium.premium.application.service;

import com.uni.coinpremium.contract.registry.Coin;
import com.uni.coinpremium.premium.application.exception.UnsupportedSymbolException;
import com.uni.coinpremium.premium.application.port.out.CoinCatalog;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 지원 코인 조회
 * - 없는 코인 → UnsupportedSymbolException (김프 조회와 같은 404 응답)
 */
@Service
public class CoinQueryService {

    private final CoinCatalog catalog;

    public CoinQueryService(CoinCatalog catalog) {
        this.catalog = catalog;
    }

    public List<Coin> getCoins() {
        return catalog.all();
    }

    public Coin getCoin(String symbol) {
        return catalog.find(symbol).orElseThrow(() -> new UnsupportedSymbolException(symbol));
    }
}
