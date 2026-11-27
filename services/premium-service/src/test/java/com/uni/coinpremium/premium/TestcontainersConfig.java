package com.uni.coinpremium.premium;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import java.io.IOException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

/**
 * 테스트 실행 시 MariaDB·Kafka·Valkey 컨테이너를 자동으로 띄우고 접속 정보를 Spring에 연결
 * - 이미지 버전은 docker-compose와 동일하게 맞춤
 * - Valkey는 Toxiproxy를 거쳐 연결 → 장애 테스트에서 지연·끊김을 주입할 수 있음 (평소엔 그대로 통과)
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    private static final int PROXY_PORT = 8666;   // Toxiproxy 컨테이너가 미리 열어 두는 첫 번째 포트

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

    @Bean
    Network network() {
        return Network.newNetwork();   // Valkey와 Toxiproxy가 서로 이름으로 찾을 수 있게 같은 네트워크
    }

    @Bean
    GenericContainer<?> valkey(Network network) {
        return new GenericContainer<>("valkey/valkey:9.0")
                .withExposedPorts(6379)
                .withNetwork(network)
                .withNetworkAliases("valkey");   // Toxiproxy가 "valkey:6379"로 접근
    }

    @Bean
    ToxiproxyContainer toxiproxy(Network network) {
        return new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0").withNetwork(network);
    }

    /** 장애 테스트에서 주입받아 지연·끊김을 거는 손잡이 */
    @Bean
    Proxy valkeyProxy(ToxiproxyContainer toxiproxy, GenericContainer<?> valkey) throws IOException {
        valkey.start();
        toxiproxy.start();
        ToxiproxyClient client = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
        return client.createProxy("valkey", "0.0.0.0:" + PROXY_PORT, "valkey:6379");
    }

    /** premium이 Valkey 대신 Toxiproxy 주소로 연결하게 */
    @Bean
    DynamicPropertyRegistrar valkeyProperties(ToxiproxyContainer toxiproxy, Proxy valkeyProxy) {
        return registry -> {
            registry.add("spring.data.redis.host", toxiproxy::getHost);
            registry.add("spring.data.redis.port", () -> toxiproxy.getMappedPort(PROXY_PORT));
        };
    }
}