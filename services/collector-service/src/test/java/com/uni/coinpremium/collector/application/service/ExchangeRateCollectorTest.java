package com.uni.coinpremium.collector.application.service;

import com.uni.coinpremium.collector.application.port.out.ExchangeRateFetcher;
import com.uni.coinpremium.contract.ExchangeRateEvent;
import com.uni.coinpremium.contract.FiatCurrency;
import com.uni.coinpremium.contract.RateSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExchangeRateCollectorTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Clock SATURDAY = Clock.fixed(Instant.parse("2026-11-07T03:00:00Z"), KST);   // 토요일 정오
    private static final LocalDate FRIDAY = LocalDate.of(2026, 11, 6);

    private final List<ExchangeRateEvent> published = new ArrayList<>();

    private ExchangeRateCollector collector(ExchangeRateFetcher fetcher) {
        return new ExchangeRateCollector(fetcher, published::add, SATURDAY);
    }

    private static ExchangeRateEvent rateOf(LocalDate date) {
        return new ExchangeRateEvent("KOREAEXIM-USD-KRW-" + date, FiatCurrency.USD, FiatCurrency.KRW,
                new BigDecimal("1340.7"), RateSource.KOREAEXIM,
                date.atStartOfDay(KST).toInstant(), Instant.now());
    }

    @Test
    @DisplayName("주말이면 직전 영업일(금요일) 환율을 발행한다")
    void lookbackOnWeekend() {
        var collector = collector(date ->
                date.equals(FRIDAY) ? Optional.of(rateOf(FRIDAY)) : Optional.empty());

        collector.collectLatest();

        assertEquals(1, published.size());
        assertEquals("KOREAEXIM-USD-KRW-" + FRIDAY, published.get(0).eventId());
    }

    @Test
    @DisplayName("같은 환율은 다시 발행하지 않는다")
    void skipDuplicate() {
        var collector = collector(date -> Optional.of(rateOf(date)));

        collector.collectLatest();
        collector.collectLatest();

        assertEquals(1, published.size());
    }
}