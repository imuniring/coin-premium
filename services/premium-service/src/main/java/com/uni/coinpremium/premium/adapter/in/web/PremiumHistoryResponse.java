package com.uni.coinpremium.premium.adapter.in.web;

import com.uni.coinpremium.premium.application.service.PremiumHistory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * GET /v1/coins/{symbol}/premium/history 응답
 */
public record PremiumHistoryResponse(
        String symbol,
        String interval,
        Instant from,
        Instant to,
        List<Item> items
) {

    public record Item(Instant time, BigDecimal open, BigDecimal high, BigDecimal low,
                       BigDecimal close, BigDecimal avg, int count) {}

    static PremiumHistoryResponse from(PremiumHistory h) {
        List<Item> items = h.candles().stream()
                .map(c -> new Item(c.time(), c.open(), c.high(), c.low(), c.close(), c.avg(), c.count()))
                .toList();
        return new PremiumHistoryResponse(h.symbol(), h.interval().code(), h.from(), h.to(), items);
    }
}