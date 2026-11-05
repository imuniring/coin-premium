package com.uni.coinpremium.collector.adapter.out.kafka;

import com.uni.coinpremium.contract.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * 토픽이 없으면 앱 기동 시 생성
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic tickerTopic() {
        return TopicBuilder.name(Topics.TICKER)
                .partitions(3)
                .replicas(1)   // 로컬 브로커 1대
                .build();
    }
}