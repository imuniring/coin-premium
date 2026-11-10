package com.uni.coinpremium.premium.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * 김치프리미엄
 * 김프(%) = (국내 가격 ÷ (해외 가격 × 환율) − 1) × 100
 *
 * @param symbol          코인 심볼 (예: BTC)
 * @param domesticPrice   국내 원화 가격
 * @param foreignPrice    해외 USDT 가격
 * @param exchangeRate    USD→KRW 환율 (USDT ≈ USD로 근사)
 * @param foreignPriceKrw 해외 가격의 원화 환산값
 * @param rate            김프율 (%, 소수 4자리)
 * @param calculatedAt    계산 시각
 */
public record KimchiPremium(
        String symbol,
        BigDecimal domesticPrice,
        BigDecimal foreignPrice,
        BigDecimal exchangeRate,
        BigDecimal foreignPriceKrw,
        BigDecimal rate,
        Instant calculatedAt
) {

    private static final int RATIO_SCALE = 10;   // 중간 비율 정밀도
    private static final int RATE_SCALE = 4;     // 최종 김프율 소수 자릿수
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * 김프 계산
     */
    public static KimchiPremium calculate(String symbol, BigDecimal domesticPrice,
                                          BigDecimal foreignPrice, BigDecimal exchangeRate,
                                          Instant calculatedAt) {
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(calculatedAt, "calculatedAt");
        requirePositive(domesticPrice, "domesticPrice");
        requirePositive(foreignPrice, "foreignPrice");
        requirePositive(exchangeRate, "exchangeRate");

        BigDecimal foreignPriceKrw = foreignPrice.multiply(exchangeRate);

        // 나눗셈은 자릿수와 반올림 방식을 반드시 지정 (안 하면 무한소수에서 예외)
        BigDecimal ratio = domesticPrice.divide(foreignPriceKrw, RATIO_SCALE, RoundingMode.HALF_UP);
        BigDecimal rate = ratio.subtract(BigDecimal.ONE)
                .multiply(HUNDRED)
                .setScale(RATE_SCALE, RoundingMode.HALF_UP);

        return new KimchiPremium(symbol, domesticPrice, foreignPrice, exchangeRate,
                foreignPriceKrw, rate, calculatedAt);
    }

    private static void requirePositive(BigDecimal value, String name) {
        Objects.requireNonNull(value, name);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
    }
}