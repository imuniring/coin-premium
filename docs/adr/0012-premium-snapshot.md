# 0012. 1분 단위 김프 스냅샷 저장

- 상태: 채택
- 날짜: 2026-11-11

## 맥락
실시간 조회만으로는 과거 김프 추이를 볼 수 없고, 메모리 값은 재시작하면 사라진다.

## 결정
- 매 분 0초(`cron`)에 지원 코인 전체의 김프를 `premium_snapshot`에 저장
- 계산은 조회 API와 같은 `PremiumQueryService`를 재사용
- 기준 시각(`captured_at`)은 분 단위로 자른 UTC
- 테이블 설계
  - 금액 `DECIMAL`, 시각 `DATETIME(6)` (UTC 저장)
  - `UNIQUE (symbol, captured_at)` + jOOQ `onDuplicateKeyIgnore()` (MariaDB `INSERT IGNORE`)
- 코인별로 독립 저장, 하나의 트랜잭션으로 묶지 않음
- 데이터 준비 전 코인은 건너뛰고 로그만 남김

## 이유
- `Instant` → `LocalDateTime` 변환을 `ZoneOffset.UTC`로 고정. 시스템 기본 시간대를 쓰면 실행 환경마다 9시간 차이가 생김
- `TIMESTAMP`는 2038년 한계와 세션 시간대 변환이 있어 `DATETIME` 사용
- UNIQUE + INSERT IGNORE로 다중 인스턴스가 동시에 저장해도 같은 시점은 1건만 남음
- 코인끼리 서로 무관하므로, 한 코인 실패가 다른 코인 저장을 롤백하지 않도록 함
- 스냅샷은 정각 정렬이 중요하고 실행 시간이 짧아 `fixedDelay` 대신 `cron` (collector 환율은 겹침 방지가 중요해 `fixedDelay`)

## 검증
- `UTC_TIMESTAMP()`와 마지막 `captured_at` 비교 (14:52:14 vs 14:52:00)
- 통합 테스트: 중복 저장 무시, UTC 저장 확인

## 트레이드오프 / 다음 과제
- 서비스가 꺼져 있던 구간은 비어 있음 → 2단계 공백 감지·보정, `source`(LIVE/BACKFILL) 컬럼 추가
- 데이터가 쌓이면 보관 정책·파티셔닝 검토
