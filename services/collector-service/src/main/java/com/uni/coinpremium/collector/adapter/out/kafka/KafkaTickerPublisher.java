package com.uni.coinpremium.collector.adapter.out.kafka;

import com.uni.coinpremium.collector.application.port.out.TickerPublisher;
import com.uni.coinpremium.contract.TickerEvent;
import com.uni.coinpremium.contract.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

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
        // 콜백은 프로듀서 내부 스레드에서 실행될 수 있어 MDC가 비어 있으므로, 보내기 전에 복사해 둔다
        Map<String, String> context = MDC.getCopyOfContextMap();

        // 비동기 전송: WebSocket 수신 스레드를 막지 않음
        kafkaTemplate.send(Topics.TICKER, event.symbol(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.atError()
                                .addKeyValue("eventId", event.eventId())
                                .setCause(ex)
                                .log("[kafka] 시세 발행 실패");
                    }
                });
    }

    /**
     * 복사해 둔 MDC를 붙인 채로 실행하고, 끝나면 실행 전 상태로 되돌린다.
     * clear()가 아니라 되돌리는 이유: 즉시 실패하면 콜백이 보낸 스레드(WebSocket)에서
     * 바로 실행될 수 있고, 그때 clear()하면 그 스레드의 MDC까지 지워진다.
     */
    private static void withContext(Map<String, String> context, Runnable action) {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        if (context != null) {
            MDC.setContextMap(context);
        }
        try {
            action.run();
        } finally {
            if (previous != null) {
                MDC.setContextMap(previous);
            } else {
                MDC.clear();   // 원래 비어 있던 스레드(프로듀서 내부)라면 비워서 돌려놓기
            }
        }
    }
}