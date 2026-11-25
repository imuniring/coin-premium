package com.uni.coinpremium.collector.adapter.out.kafka;

import com.uni.coinpremium.collector.application.port.out.TickerPublisher;
import com.uni.coinpremium.contract.TickerEvent;
import com.uni.coinpremium.contract.Topics;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 시세를 Kafka로 전송 (BufferedTickerPublisher의 전담 스레드에서만 호출)
 * - send()가 동기 예외로 실패하면 그대로 던져 호출자가 재시도하게 한다
 * - 비동기 실패(전송 시간 초과)는 콜백에서 기록
 */
@Component
public class KafkaTickerPublisher implements TickerPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaTickerPublisher.class);

    // 메트릭 이름 (Actuator: /actuator/metrics/{이름})
    private static final String PUBLISHED = "collector.ticker.published";   // Kafka 전송 결과 수

    private final KafkaTemplate<String, TickerEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public KafkaTickerPublisher(KafkaTemplate<String, TickerEvent> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void publish(TickerEvent event) {
        String exchange = event.exchange().name();

        // 콜백은 프로듀서 내부 스레드에서 실행될 수 있어 MDC가 비어 있으므로, 보내기 전에 복사해 둔다
        Map<String, String> context = MDC.getCopyOfContextMap();
        try {
            // 비동기 전송 (단, 메타데이터가 없으면 max.block.ms 동안 호출 스레드를 블로킹함 → #42)
            kafkaTemplate.send(Topics.TICKER, event.symbol(), event)
                    .whenComplete((result, ex) -> {
                        countPublished(exchange, ex == null);
                        if (ex != null) {
                            withContext(context, () ->
                                    log.atError()
                                            .addKeyValue("eventId", event.eventId())
                                            .setCause(ex)
                                            .log("[kafka] 시세 발행 실패"));
                        }
                    });
        } catch (RuntimeException e) {
            // send() 자체가 동기 예외로 실패한 경우 (메타데이터 대기 시간 초과 등)
            countPublished(exchange, false);
            throw e;   // 기존처럼 WebSocket dispatch의 catch에서 기록
        }
    }

    private void countPublished(String exchange, boolean success) {
        meterRegistry.counter(PUBLISHED, "exchange", exchange, "result", success ? "success" : "failure")
                .increment();
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