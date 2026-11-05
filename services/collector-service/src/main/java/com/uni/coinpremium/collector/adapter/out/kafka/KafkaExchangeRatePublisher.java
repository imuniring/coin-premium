package com.uni.coinpremium.collector.adapter.out.kafka;

import com.uni.coinpremium.collector.application.port.out.ExchangeRatePublisher;
import com.uni.coinpremium.contract.ExchangeRateEvent;
import com.uni.coinpremium.contract.Topics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 환율 이벤트 Kafka 발행 (동기 전송)
 * - 시세와 달리 30분에 1건이라 성능 부담이 없고, 성공 여부를 확실히 알아야 함
 */
@Component
public class KafkaExchangeRatePublisher implements ExchangeRatePublisher {

    private final KafkaTemplate<String, ExchangeRateEvent> kafkaTemplate;

    public KafkaExchangeRatePublisher(KafkaTemplate<String, ExchangeRateEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(ExchangeRateEvent event) {
        String key = event.base() + "-" + event.target();   // "USD-KRW"
        try {
            kafkaTemplate.send(Topics.EXCHANGE_RATE, key, event).get(5, TimeUnit.SECONDS);   // 결과까지 기다림
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("환율 발행 중단 - eventId=" + event.eventId(), e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("환율 발행 실패 - eventId=" + event.eventId(), e);
        }
    }
}