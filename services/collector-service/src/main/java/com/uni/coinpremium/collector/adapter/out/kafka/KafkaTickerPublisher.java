package com.uni.coinpremium.collector.adapter.out.kafka;

import com.uni.coinpremium.collector.application.port.out.TickerPublisher;
import com.uni.coinpremium.contract.TickerEvent;
import com.uni.coinpremium.contract.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 시세 이벤트 Kafka 발행
 * - key = 심볼: 같은 코인의 국내·해외 시세가 같은 파티션에 모이도록
 */
@Component
public class KafkaTickerPublisher implements TickerPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaTickerPublisher.class);

    private final KafkaTemplate<String, TickerEvent> kafkaTemplate;

    public KafkaTickerPublisher(KafkaTemplate<String, TickerEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(TickerEvent event) {
        // 비동기 전송: WebSocket 수신 스레드를 막지 않음
        kafkaTemplate.send(Topics.TICKER, event.symbol(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[kafka] 시세 발행 실패 - eventId={}", event.eventId(), ex);
                    }
                });
    }
}