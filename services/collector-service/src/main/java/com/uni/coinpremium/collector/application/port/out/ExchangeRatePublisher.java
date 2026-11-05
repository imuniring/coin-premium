package com.uni.coinpremium.collector.application.port.out;

import com.uni.coinpremium.contract.ExchangeRateEvent;

/**
 * 환율 이벤트 발행 포트
 */
public interface ExchangeRatePublisher {
    void publish(ExchangeRateEvent event);
}