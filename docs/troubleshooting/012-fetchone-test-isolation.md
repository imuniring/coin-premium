# 012. 통합 테스트의 `fetchOne` 유일성과 테스트 독립성

- 날짜: 2026-11-11
- 관련: [ADR 0013](../adr/0013-test-strategy.md)

## 증상 (리뷰 중 발견)
```java
dsl.select(PREMIUM_SNAPSHOT.CAPTURED_AT)
   .from(PREMIUM_SNAPSHOT)
   .where(PREMIUM_SNAPSHOT.SYMBOL.eq("ETH"))
   .fetchOne(...);
```
ETH 스냅샷이 1개라는 보장이 없다.

## 원인
- `UNIQUE (symbol, captured_at)`은 같은 코인·같은 시각만 막는다. 시각이 다르면 여러 행 가능
- Spring 테스트 컨텍스트 캐시로 여러 테스트 클래스가 같은 컨테이너 DB를 공유한다
- `fetchOne`은 0건이면 null, 2건 이상이면 `TooManyRowsException`

## 해결
1. 조회 조건을 유일 키(`symbol` + `captured_at`)로 지정 → 최대 1건 보장
2. `@BeforeEach`에서 테이블을 비워 테스트 간 데이터 공유 제거

## 배운 점
- 단건 조회는 유일성이 보장되는 조건(PK, UNIQUE)으로 한다. 최신 1건이 필요하면 `orderBy(...desc()).limit(1)`로 명시
- 테스트 결과가 실행 순서에 따라 달라지면 안 된다
