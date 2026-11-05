package com.uni.coinpremium.collector.application.service;

import com.uni.coinpremium.collector.application.port.out.ExchangeRateFetchException;
import com.uni.coinpremium.collector.application.port.out.ExchangeRateFetcher;
import com.uni.coinpremium.contract.ExchangeRateEvent;
import com.uni.coinpremium.contract.FiatCurrency;
import com.uni.coinpremium.contract.RateSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ExchangeRateCollectorTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Clock SATURDAY = Clock.fixed(Instant.parse("2026-11-07T03:00:00Z"), KST);   // 토요일 정오
    private static final LocalDate FRIDAY = LocalDate.of(2026, 11, 6);

    private final List<ExchangeRateEvent> published = new ArrayList<>();

    private ExchangeRateCollector collector(ExchangeRateFetcher fetcher) {
        return new ExchangeRateCollector(fetcher, published::add, SATURDAY, Duration.ZERO);   // 재시도 대기 0
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
    @DisplayName("일시적 실패는 재시도해서 성공한다")
    void retryTransient() {
        AtomicInteger calls = new AtomicInteger();
        var collector = collector(date -> {
            if (calls.incrementAndGet() < 3) {
                throw new ExchangeRateFetchException("timeout", true);
            }
            return Optional.of(rateOf(date));
        });

        collector.collectLatest();

        assertEquals(3, calls.get());   // 실패 2번 + 성공 1번
        assertEquals(1, published.size());
    }

    @Test
    @DisplayName("영구 실패는 재시도하지 않는다")
    void noRetryOnPermanent() {
        AtomicInteger calls = new AtomicInteger();
        var collector = collector(date -> {
            calls.incrementAndGet();
            throw new ExchangeRateFetchException("auth error", false);
        });

        assertThrows(ExchangeRateFetchException.class, collector::collectLatest);
        assertEquals(1, calls.get());   // 딱 1번만 호출
        assertTrue(published.isEmpty());
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