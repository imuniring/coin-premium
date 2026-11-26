package com.uni.coinpremium.collector.adapter.in.leader;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 리더 선출 설정
 * @param ttl           열쇠 기한 (리더가 죽으면 최대 이만큼 뒤 넘겨받음)
 * @param renewInterval 연장·획득 시도 주기 (ttl보다 충분히 짧게)
 */
@ConfigurationProperties(prefix = "collector.leader")
public record LeaderProperties(Duration ttl, Duration renewInterval) {
}