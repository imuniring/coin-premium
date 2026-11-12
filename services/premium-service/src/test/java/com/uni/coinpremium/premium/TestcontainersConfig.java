package com.uni.coinpremium.premium;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mariadb.MariaDBContainer;

/**
 * 테스트 실행 시 MariaDB·Kafka 컨테이너를 자동으로 띄우고 접속 정보를 Spring에 연결
 * - 이미지 버전은 docker-compose와 동일하게 맞춤
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection   // 컨테이너의 주소·계정을 spring.datasource.* 대신 자동 연결
    MariaDBContainer mariadb() {
        return new MariaDBContainer("mariadb:11.8");
    }

    @Bean
    @ServiceConnection   // 컨테이너 주소를 spring.kafka.bootstrap-servers 대신 자동 연결
    KafkaContainer kafka() {
        return new KafkaContainer("apache/kafka:4.1.0");
    }
}