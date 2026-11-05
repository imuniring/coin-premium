package com.uni.coinpremium.contract;

/**
 * Kafka 토픽 이름 (producer, consumer 공통)
 */
public final class Topics {

    public static final String TICKER = "market.ticker.v1";
    public static final String EXCHANGE_RATE = "market.exchange-rate.v1";

    private Topics() {}
}