package com.uni.coinpremium.premium.application.service;

/**
 * 공백 보정 결과 (한 번 실행 기준)
 * @param filled      BACKFILL로 저장한 분 수
 * @param unfillable  재료가 없어 못 채운 분 수 (그 1분 체결 없음, 환율 이력 없음)
 * @param failed      거래소 조회 실패 등으로 처리하지 못한 분 수 (다음 실행 때 재시도)
 */
public record BackfillResult(int filled, int unfillable, int failed) {

    public static final BackfillResult EMPTY = new BackfillResult(0, 0, 0);

    BackfillResult plus(BackfillResult other) {
        return new BackfillResult(filled + other.filled, unfillable + other.unfillable, failed + other.failed);
    }
}