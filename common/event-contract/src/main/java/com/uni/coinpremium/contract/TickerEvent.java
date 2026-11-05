package com.uni.coinpremium.contract;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 거래소 시세 이벤트 (collector → premium)
 *
 * @param eventId    이벤트 고유 ID (중복 처리 판단용)
 * @param exchange   거래소
 * @param symbol     정규화된 코인 심볼 (예: BTC)
 * @param quote      가격 기준 통화
 * @param price      체결 가격
 * @param tradedAt   거래소 체결 시각
 * @param receivedAt collector 수신 시각
 */
public record TickerEvent(
        String eventId,
        Exchange exchange,
        String symbol,
        QuoteCurrency quote,
        BigDecimal price,
        Instant tradedAt,
        Instant receivedAt
) {

    // 잘못된 이벤트는 만들어지는 순간 막는다 (Kafka로 나가기 전에)
    public TickerEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(exchange, "exchange");
        Objects.requireNonNull(symbol, "symbol");      // ← 추가: null은 다른 필드와 똑같이
        Objects.requireNonNull(quote, "quote");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(tradedAt, "tradedAt");
        Objects.requireNonNull(receivedAt, "receivedAt");

        if (eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        if (price.signum() <= 0) {
            throw new IllegalArgumentException("price must be positive: " + price);
        }
    }

    /**
     * 새 이벤트 생성 (eventId 자동 발급)
     */
    public static TickerEvent of(Exchange exchange, String symbol, QuoteCurrency quote,
                                 BigDecimal price, Instant tradedAt, Instant receivedAt) {
        return new TickerEvent(UUID.randomUUID().toString(),
                exchange, symbol, quote, price, tradedAt, receivedAt);
    }
}