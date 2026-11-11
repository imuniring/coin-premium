package com.uni.coinpremium.premium.adapter.in.kafka;

import com.uni.coinpremium.contract.ExchangeRateEvent;
import com.uni.coinpremium.contract.TickerEvent;
import com.uni.coinpremium.contract.Topics;
import com.uni.coinpremium.premium.application.port.out.MarketDataRepository;
import com.uni.coinpremium.premium.domain.FxRate;
import com.uni.coinpremium.premium.domain.PriceQuote;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final JsonMapper jsonMapper;
    private final MarketDataRepository repository;

    public MarketDataKafkaListener(JsonMapper jsonMapper, MarketDataRepository repository) {
        this.jsonMapper = jsonMapper;
        this.repository = repository;
    }

    @KafkaListener(topics = Topics.TICKER)
    public void onTicker(String payload) {
        try {
            TickerEvent event = jsonMapper.readValue(payload, TickerEvent.class);
            repository.saveQuote(new PriceQuote(
                    event.exchange().name(), event.symbol(), event.price(), event.tradedAt()));
            log.debug("[kafka] 시세 반영 - {} {} {}", event.exchange(), event.symbol(), event.price());
        } catch (Exception e) {
            // 불량 메시지 1건 때문에 컨슈머가 멈추지 않도록 기록 후 건너뜀
            log.warn("[kafka] 시세 메시지 처리 실패 - payload={}", payload, e);
        }
    }

    @KafkaListener(topics = Topics.EXCHANGE_RATE)
    public void onExchangeRate(String payload) {
        try {
            ExchangeRateEvent event = jsonMapper.readValue(payload, ExchangeRateEvent.class);
            repository.saveFxRate(new FxRate(event.rate(), event.publishedAt()));
            log.info("[kafka] 환율 반영 - rate={}, eventId={}", event.rate(), event.eventId());
        } catch (Exception e) {
            log.warn("[kafka] 환율 메시지 처리 실패 - payload={}", payload, e);
        }
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