package com.uni.coinpremium.premium.application.service;

import com.uni.coinpremium.premium.application.exception.MarketDataNotReadyException;
import com.uni.coinpremium.premium.application.exception.UnsupportedSymbolException;
import com.uni.coinpremium.premium.application.port.out.MarketDataRepository;
import com.uni.coinpremium.premium.config.PremiumProperties;
import com.uni.coinpremium.premium.domain.FxRate;
import com.uni.coinpremium.premium.domain.KimchiPremium;
import com.uni.coinpremium.premium.domain.PriceQuote;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 김프 조회
 * - 지원하지 않는 코인 → UnsupportedSymbolException
 * - 시세·환율 중 하나라도 없음 → MarketDataNotReadyException (무엇이 없는지 포함)
 */
@Service
public class PremiumQueryService {

    private static final String DOMESTIC = "UPBIT";
    private static final String FOREIGN = "BINANCE";
    private static final String FX_RATE = "FX_RATE";

    private final MarketDataRepository repository;
    private final PremiumProperties properties;
    private final Clock clock;

    public PremiumQueryService(MarketDataRepository repository, PremiumProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    public PremiumSnapshot getPremium(String symbol) {
        if (!properties.supports(symbol)) {
            throw new UnsupportedSymbolException(symbol);
        }

        Optional<PriceQuote> domestic = repository.findQuote(DOMESTIC, symbol);
        Optional<PriceQuote> foreign = repository.findQuote(FOREIGN, symbol);
        Optional<FxRate> fxRate = repository.findFxRate();

        // 무엇이 빠졌는지 전부 모아서 한 번에 알려줌 (하나씩 알려주면 디버깅이 번거로움)
        List<String> missing = new ArrayList<>();
        if (domestic.isEmpty()) missing.add(DOMESTIC);
        if (foreign.isEmpty()) missing.add(FOREIGN);
        if (fxRate.isEmpty()) missing.add(FX_RATE);
        if (!missing.isEmpty()) {
            throw new MarketDataNotReadyException(symbol, missing);
        }

        PriceQuote d = domestic.orElseThrow();
        PriceQuote f = foreign.orElseThrow();
        FxRate fx = fxRate.orElseThrow();

        KimchiPremium premium = KimchiPremium.calculate(symbol, d.price(), f.price(), fx.rate(), clock.instant());
        return new PremiumSnapshot(premium, d, f, fx);
    }
}