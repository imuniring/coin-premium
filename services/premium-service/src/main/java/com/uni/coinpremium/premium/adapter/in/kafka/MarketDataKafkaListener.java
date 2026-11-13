package com.uni.coinpremium.premium.adapter.in.kafka;

import com.uni.coinpremium.contract.ExchangeRateEvent;
import com.uni.coinpremium.contract.TickerEvent;
import com.uni.coinpremium.contract.Topics;
import com.uni.coinpremium.premium.application.port.out.MarketDataRepository;
import com.uni.coinpremium.premium.domain.FxRate;
import com.uni.coinpremium.premium.domain.PriceQuote;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/**
 * 시세·환율 이벤트 수신 → 최신값 저장
 */
@Component
public class MarketDataKafkaListener implements ConsumerSeekAware {

    private static final Logger log = LoggerFactory.getLogger(MarketDataKafkaListener.class);

    private static final String MDC_KAFKA_RECORD = "kafkaRecord";   // 파싱 전에도 알 수 있는 메시지 위치
    private static final String MDC_EVENT_ID = "eventId";           // 파싱 후 알 수 있는 업무 ID

    private final JsonMapper jsonMapper;
    private final MarketDataRepository repository;

    public MarketDataKafkaListener(JsonMapper jsonMapper, MarketDataRepository repository) {
        this.jsonMapper = jsonMapper;
        this.repository = repository;
    }

    @KafkaListener(topics = Topics.TICKER)
    public void onTicker(ConsumerRecord<String, String> record) {
        MDC.put(MDC_KAFKA_RECORD, location(record));
        try {
            TickerEvent event = jsonMapper.readValue(record.value(), TickerEvent.class);
            MDC.put(MDC_EVENT_ID, event.eventId());
            repository.saveQuote(new PriceQuote(
                    event.exchange().name(), event.symbol(), event.price(), event.tradedAt()));
            log.debug("[kafka] 시세 반영 - {} {} {}", event.exchange(), event.symbol(), event.price());
        } catch (Exception e) {
            // 불량 메시지 1건 때문에 컨슈머가 멈추지 않도록 기록 후 건너뜀
            log.warn("[kafka] 시세 메시지 처리 실패 - payload={}", record.value(), e);
        } finally {
            clearMdc(); // 리스너 스레드는 재사용되므로 다음 메시지에 값이 남지 않게
        }
    }

    @KafkaListener(topics = Topics.EXCHANGE_RATE)
    public void onExchangeRate(ConsumerRecord<String, String> record) {
        MDC.put(MDC_KAFKA_RECORD, location(record));
        try {
            ExchangeRateEvent event = jsonMapper.readValue(record.value(), ExchangeRateEvent.class);
            MDC.put(MDC_EVENT_ID, event.eventId());
            repository.saveFxRate(new FxRate(event.rate(), event.publishedAt()));
            log.info("[kafka] 환율 반영 - rate={}", event.rate());
        } catch (Exception e) {
            log.warn("[kafka] 환율 메시지 처리 실패 - payload={}", record.value(), e);
        } finally {
            clearMdc();
        }
    }

    // 예: market.ticker.v1-0@1523 (Kafka UI에서 이 위치로 메시지를 바로 찾을 수 있음)
    private static String location(ConsumerRecord<?, ?> record) {
        return record.topic() + "-" + record.partition() + "@" + record.offset();
    }

    private static void clearMdc() {
        MDC.remove(MDC_KAFKA_RECORD);
        MDC.remove(MDC_EVENT_ID);
    }

    /**
     * 파티션을 받을 때 읽기 시작 위치 지정
     * - 환율: 처음부터 (재시작하면 메모리가 비니까, 지난 환율을 다시 읽어야 함)
     * - 시세: 맨 끝부터 (초당 수십 건 들어오니 밀린 과거 시세는 필요 없음)
     */
    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        assignments.keySet().forEach(tp -> {
            if (Topics.EXCHANGE_RATE.equals(tp.topic())) {
                callback.seekToBeginning(tp.topic(), tp.partition());
            } else {
                callback.seekToEnd(tp.topic(), tp.partition());
            }
        });
    }
}