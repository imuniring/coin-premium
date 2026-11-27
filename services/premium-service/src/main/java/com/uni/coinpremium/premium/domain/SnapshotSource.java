package com.uni.coinpremium.premium.domain;

/**
 * 스냅샷 출처
 */
public enum SnapshotSource {
    LIVE,       // 매분 실시간 시세로 저장
    BACKFILL    // 공백을 나중에 거래소 과거 분봉으로 채움
}