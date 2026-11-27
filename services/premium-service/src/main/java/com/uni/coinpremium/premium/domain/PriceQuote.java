package com.uni.coinpremium.premium.domain;

import java.math.BigDecimal;
import java.time.Duration;
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

    /** now 기준으로 maxAge보다 오래된 체결이면 true (collector 중단 등으로 시세가 멈춘 상태) */
    public boolean isStale(Instant now, Duration maxAge) {
        return tradedAt.isBefore(now.minus(maxAge));
    }
}