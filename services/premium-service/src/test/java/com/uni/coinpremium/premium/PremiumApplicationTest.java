package com.uni.coinpremium.premium;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class PremiumApplicationTest {

    @Test
    void contextLoads() {
        // 컨테이너 DB에 Flyway 적용 + Kafka 연결까지 포함해서 앱이 뜨는지 확인
    }
}