package com.uni.coinpremium.premium.adapter.out.memory;

import com.uni.coinpremium.premium.application.port.out.MarketDataRepository;
import com.uni.coinpremium.premium.domain.FxRate;
import com.uni.coinpremium.premium.domain.PriceQuote;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 메모리 저장소: Kafka 컨슈머 스레드가 쓰고, API 스레드가 읽는다
 */
@Repository
public class InMemoryMarketDataRepository implements MarketDataRepository {

    // key = "UPBIT:BTC"
    private final ConcurrentHashMap<String, PriceQuote> quotes = new ConcurrentHashMap<>();
    private final AtomicReference<FxRate> fxRate = new AtomicReference<>();

    @Override
    public void saveQuote(PriceQuote quote) {
        // merge: "기존 값과 비교해서 더 최신이면 교체"를 한 번에(원자적으로) 처리
        quotes.merge(key(quote.exchange(), quote.symbol()), quote,
                (old, incoming) -> incoming.isNewerThan(old) ? incoming : old);
    }

    @Override
    public Optional<PriceQuote> findQuote(String exchange, String symbol) {
        return Optional.ofNullable(quotes.get(key(exchange, symbol)));
    }

    @Override
    public void saveFxRate(FxRate rate) {
        // 첫 값이면 그대로, 아니면 더 최신일 때만 교체
        fxRate.accumulateAndGet(rate,
                (old, incoming) -> old == null || incoming.isNewerThan(old) ? incoming : old);
    }

    @Override
    public Optional<FxRate> findFxRate() {
        return Optional.ofNullable(fxRate.get());
    }

    private static String key(String exchange, String symbol) {
        return exchange + ":" + symbol;
    }
}