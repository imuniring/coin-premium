package com.uni.coinpremium.premium.adapter.in.web;

import com.uni.coinpremium.premium.application.service.PremiumSnapshot;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * GET /premium 응답
 */
public record PremiumResponse(
        String symbol,
        BigDecimal premiumRate,       // 김프 (%)
        Market domestic,
        Market foreign,
        Fx exchangeRate,
        BigDecimal foreignPriceKrw,   // 해외 가격 원화 환산
        Instant calculatedAt
) {

    public record Market(String exchange, BigDecimal price, Instant tradedAt) {}

    public record Fx(BigDecimal rate, Instant publishedAt) {}

    static PremiumResponse from(PremiumSnapshot s) {
        return new PremiumResponse(
                s.premium().symbol(),
                s.premium().rate(),
                new Market(s.domestic().exchange(), s.domestic().price(), s.domestic().tradedAt()),
                new Market(s.foreign().exchange(), s.foreign().price(), s.foreign().tradedAt()),
                new Fx(s.fxRate().rate(), s.fxRate().publishedAt()),
                s.premium().foreignPriceKrw(),
                s.premium().calculatedAt()
        );
    }
}