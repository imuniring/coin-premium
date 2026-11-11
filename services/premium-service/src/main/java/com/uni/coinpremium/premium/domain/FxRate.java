package com.uni.coinpremium.premium.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * 최신 USD→KRW 환율
 */
public record FxRate(BigDecimal rate, Instant publishedAt) {

    public FxRate {
        Objects.requireNonNull(rate, "rate");
        Objects.requireNonNull(publishedAt, "publishedAt");
    }

    public boolean isNewerThan(FxRate other) {
        return !publishedAt.isBefore(other.publishedAt);
    }
}