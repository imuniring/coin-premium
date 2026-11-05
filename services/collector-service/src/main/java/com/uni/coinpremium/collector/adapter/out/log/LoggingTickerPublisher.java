package com.uni.coinpremium.collector.adapter.out.log;

import com.uni.coinpremium.collector.application.port.out.TickerPublisher;
import com.uni.coinpremium.contract.TickerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 임시 발행기: Kafka 연동 전까지 로그로만 출력
 */
@Component
public class LoggingTickerPublisher implements TickerPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingTickerPublisher.class);

    @Override
    public void publish(TickerEvent event) {
        log.info("[publish] {} {} {}/{} price={} tradedAt={}",
                event.eventId(), event.exchange(), event.symbol(), event.quote(), event.price(), event.tradedAt());
    }
}