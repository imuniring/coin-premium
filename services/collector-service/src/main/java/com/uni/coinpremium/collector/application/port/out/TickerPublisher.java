package com.uni.coinpremium.collector.application.port.out;

import com.uni.coinpremium.contract.TickerEvent;

/**
 * 시세 이벤트 발행 포트 (구현: 로그, Kafka 등)
 */
public interface TickerPublisher {
    void publish(TickerEvent event);
}