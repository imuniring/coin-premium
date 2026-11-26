package com.uni.coinpremium.premium.adapter.in.scheduler;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 스케줄러 분산 락: 여러 인스턴스 중 열쇠를 먼저 잡은 한 대만 실행
 * - 락은 Valkey에 저장 (키: job-lock:premium:{락 이름})
 * - 기본 최대 보유 시간 50초: 락을 잡은 인스턴스가 죽어도 다음 실행 전에 자동 해제
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "50s")
public class SchedulerLockConfig {

    @Bean
    LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "premium");   // 키 앞에 서비스 이름을 붙여 다른 서비스 락과 구분
    }
}
