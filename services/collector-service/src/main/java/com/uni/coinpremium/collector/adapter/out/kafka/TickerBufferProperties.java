package com.uni.coinpremium.collector.adapter.out.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 시세 발행 보관함 설정
 * @param capacity     보관함 최대 건수 (Kafka 장애를 버틸 시간 × 초당 수신량)
 * @param retryBackoff 전송 실패 후 같은 체결을 다시 보내기까지 대기
 */
@ConfigurationProperties(prefix = "collector.ticker-buffer")
public record TickerBufferProperties(int capacity, Duration retryBackoff) {
}
