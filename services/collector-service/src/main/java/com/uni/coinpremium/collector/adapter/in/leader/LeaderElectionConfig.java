package com.uni.coinpremium.collector.adapter.in.leader;

import com.uni.coinpremium.collector.adapter.in.websocket.ExchangeWebSocketClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Configuration
public class LeaderElectionConfig {

    @Bean
    LeaderElection leaderElection(StringRedisTemplate redis, LeaderProperties properties,
                                  List<ExchangeWebSocketClient> clients) {
        // 같은 컴퓨터에서 여러 대 띄워도 구분되도록 호스트 이름 + 임의 값
        String host = Optional.ofNullable(System.getenv("HOSTNAME")).orElse("local");
        String instanceId = host + "-" + UUID.randomUUID().toString().substring(0, 8);

        return new LeaderElection(redis, properties, instanceId,
                () -> clients.forEach(ExchangeWebSocketClient::activate),
                () -> clients.forEach(ExchangeWebSocketClient::deactivate));
    }
}
