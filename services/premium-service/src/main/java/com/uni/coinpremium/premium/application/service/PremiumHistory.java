package com.uni.coinpremium.premium.application.service;

import com.uni.coinpremium.premium.domain.HistoryInterval;
import com.uni.coinpremium.premium.domain.PremiumCandle;

import java.time.Instant;
import java.util.List;

public record PremiumHistory(
        String symbol,
        HistoryInterval interval,
        Instant from,
        Instant to,
        List<PremiumCandle> candles
) {}