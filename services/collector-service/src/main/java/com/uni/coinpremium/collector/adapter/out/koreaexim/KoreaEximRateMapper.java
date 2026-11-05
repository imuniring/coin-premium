package com.uni.coinpremium.collector.adapter.out.koreaexim;

import com.uni.coinpremium.contract.ExchangeRateEvent;
import com.uni.coinpremium.contract.FiatCurrency;
import com.uni.coinpremium.contract.RateSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;

/**
 * 수출입은행 응답 → ExchangeRateEvent 변환
 */
final class KoreaEximRateMapper {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private KoreaEximRateMapper() {}

    static Optional<ExchangeRateEvent> toUsdKrw(KoreaEximRate[] rates, LocalDate date, Instant receivedAt) {
        // 주말·공휴일·고시 전에는 빈 배열
        if (rates == null || rates.length == 0) {
            return Optional.empty();
        }

        // 오류 응답은 result 코드로 옴 (인증 실패, 일일 제한 초과 등)
        Integer result = rates[0].result();
        if (result == null || result != 1) {
            throw new IllegalStateException("koreaexim error result=" + result);
        }

        KoreaEximRate usd = Arrays.stream(rates)
                .filter(r -> "USD".equals(r.curUnit()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("USD rate not found"));

        BigDecimal rate = new BigDecimal(usd.dealBasR().replace(",", ""));   // "1,369.4" → 1369.4

        return Optional.of(new ExchangeRateEvent(
                "KOREAEXIM-USD-KRW-" + date.format(DateTimeFormatter.BASIC_ISO_DATE),   // 같은 날짜 = 같은 ID
                FiatCurrency.USD,
                FiatCurrency.KRW,
                rate,
                RateSource.KOREAEXIM,
                date.atStartOfDay(KST).toInstant(),   // API가 고시 시각을 안 줘서 해당 날짜 0시(KST)로 기록
                receivedAt
        ));
    }
}