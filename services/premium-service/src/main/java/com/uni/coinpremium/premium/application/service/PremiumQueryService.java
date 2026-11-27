package com.uni.coinpremium.premium.application.service;

import com.uni.coinpremium.premium.application.exception.MarketDataNotReadyException;
import com.uni.coinpremium.premium.application.exception.MarketDataUnavailableException;
import com.uni.coinpremium.premium.application.exception.UnsupportedSymbolException;
import com.uni.coinpremium.premium.application.port.out.MarketDataRepository;
import com.uni.coinpremium.premium.application.port.out.SupportedCoins;
import com.uni.coinpremium.premium.config.FreshnessProperties;
import com.uni.coinpremium.premium.domain.FxRate;
import com.uni.coinpremium.premium.domain.KimchiPremium;
import com.uni.coinpremium.premium.domain.PriceQuote;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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

    // 코인별 마지막으로 계산한 김프 (Valkey 장애 시 stale로 응답하기 위해 인스턴스 메모리에 보관)
    private final Map<String, PremiumSnapshot> lastKnown = new ConcurrentHashMap<>();

    private final MarketDataRepository repository;
    private final SupportedCoins supportedCoins;
    private final Duration maxQuoteAge;
    private final Clock clock;

    public PremiumQueryService(MarketDataRepository repository, SupportedCoins supportedCoins,
                               FreshnessProperties freshness, Clock clock) {
        this.repository = repository;
        this.supportedCoins = supportedCoins;
        this.maxQuoteAge = freshness.maxQuoteAge();
        this.clock = clock;
    }

    public PremiumSnapshot getPremium(String symbol) {
        if (!supportedCoins.supports(symbol)) {
            throw new UnsupportedSymbolException(symbol);
        }
        try {
            PremiumSnapshot snapshot = calculate(symbol);
            lastKnown.put(symbol, snapshot);
            return snapshot;
        } catch (MarketDataUnavailableException e) {
            // 저장소 장애: 더 새로운 값이 있는지 확인할 수 없으므로 마지막 값을 오래된 값으로 응답
            PremiumSnapshot last = lastKnown.get(symbol);
            if (last == null) {
                throw e;   // 기억해 둔 값도 없음 → 503
            }
            return last.asStale();
        }
    }

    private PremiumSnapshot calculate(String symbol) {
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

        Instant now = clock.instant();
        KimchiPremium premium = KimchiPremium.calculate(symbol, d.price(), f.price(), fx.rate(), clock.instant());
        boolean stale = d.isStale(now, maxQuoteAge) || f.isStale(now, maxQuoteAge);
        return new PremiumSnapshot(premium, d, f, fx, stale);
    }
}