package com.uni.coinpremium.premium.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * 거래소의 최신 시세 1건
 */
public record PriceQuote(
        String exchange,     // UPBIT, BINANCE
        String symbol,       // BTC
        BigDecimal price,
        Instant tradedAt
) {
    public PriceQuote {
        Objects.requireNonNull(exchange, "exchange");
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(tradedAt, "tradedAt");
    }

    /** other보다 최신이면 true (같은 시각이면 나중에 온 값으로 교체) */
    public boolean isNewerThan(PriceQuote other) {
        return !tradedAt.isBefore(other.tradedAt);
    }
}