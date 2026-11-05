package com.uni.coinpremium.contract;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 환율 이벤트 (collector → premium)
 *
 * @param eventId     이벤트 고유 ID (중복 처리 판단용)
 * @param base        기준 통화 (예: USD)
 * @param target      대상 통화 (예: KRW)
 * @param rate        환율 (1 base = rate target)
 * @param source      환율 출처
 * @param publishedAt 환율 고시 시각
 * @param receivedAt  collector 수신 시각
 */
public record ExchangeRateEvent(
        String eventId,
        FiatCurrency base,
        FiatCurrency target,
        BigDecimal rate,
        RateSource source,
        Instant publishedAt,
        Instant receivedAt
) {

    public ExchangeRateEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(rate, "rate");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(publishedAt, "publishedAt");
        Objects.requireNonNull(receivedAt, "receivedAt");

        if (eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        if (base == target) {   // USD → USD 같은 환율은 의미 없음
            throw new IllegalArgumentException("base and target must differ: " + base);
        }
        if (rate.signum() <= 0) {
            throw new IllegalArgumentException("rate must be positive: " + rate);
        }
    }

    /**
     * 새 이벤트 생성 (eventId 자동 발급)
     */
    public static ExchangeRateEvent of(FiatCurrency base, FiatCurrency target, BigDecimal rate,
                                       RateSource source, Instant publishedAt, Instant receivedAt) {
        return new ExchangeRateEvent(UUID.randomUUID().toString(),
                base, target, rate, source, publishedAt, receivedAt);
    }
}