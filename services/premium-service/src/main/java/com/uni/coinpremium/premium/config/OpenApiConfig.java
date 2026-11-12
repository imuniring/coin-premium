package com.uni.coinpremium.premium.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 문서 기본 정보
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI premiumOpenApi() {
        return new OpenAPI().info(new Info()
                .title("coin-premium API")
                .version("v1")
                .description("""
                        국내·해외 거래소 실시간 시세와 공식 환율로 계산한 김치프리미엄 조회 API.
                        시각은 ISO-8601 UTC, 에러는 ProblemDetail(RFC 9457) 형식이며 `code`로 구분한다."""));
    }
}