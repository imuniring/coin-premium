# 015. try-with-resources의 catch에서 MDC가 사라짐

- 날짜: 2026-11-15
- 관련: #34, ADR 0017

## 증상
`MDC.putCloseable`을 try-with-resources로 감싸고 같은 try에 catch를 붙이면,
정작 실패 로그에 `job`, `jobId`가 빠진다.

```java
// ❌ 실패 로그에 jobId가 없음
try (var jobId = MDC.putCloseable("jobId", id)) {
    collector.collectLatest();
} catch (Exception e) {
    log.error("[exchange-rate] 환율 수집 실패", e);
}
```

## 원인
try-with-resources는 **catch 블록에 들어가기 전에 자원을 먼저 닫는다.**
`MDCCloseable.close()`가 `MDC.remove`를 호출하므로, catch에서는 이미 값이 지워진 상태다.

## 해결
MDC를 바깥 try에서 열고, 기존 try-catch를 안쪽에 둔다.

```java
try (var jobId = MDC.putCloseable("jobId", id)) {
    try {
        collector.collectLatest();
    } catch (Exception e) {
        log.error("[exchange-rate] 환율 수집 실패", e);   // jobId 유지
    }
}
```

값을 넣는 시점이 중간에 정해지는 경우(Kafka 리스너의 `eventId`)는 `put` + `finally`로 정리한다.

## 배운 점
- 에러 로그는 추적 ID가 가장 필요한 곳이다. MDC 범위가 catch까지 덮는지 확인한다.
- try-with-resources의 실행 순서: try 본문 → 자원 close → catch → finally.
