package com.uni.coinpremium.premium.config;

import com.uni.coinpremium.premium.domain.FxSwitchRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();   // 저장·계산은 UTC
    }

    @Bean
    FxSwitchRule fxSwitchRule(FxProperties properties) {
        return new FxSwitchRule(properties.switchTime(), ZoneId.of("Asia/Seoul"));
    }
}