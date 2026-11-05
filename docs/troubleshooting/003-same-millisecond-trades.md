# 003. 같은 시각·같은 가격 체결이 중복처럼 보임

- 날짜: 2026-11-05
- 관련: [ADR 0008](../adr/0008-deterministic-event-id.md)

## 증상
발행 로그에 `tradedAt`과 `price`가 완전히 같은 Upbit ETH 체결이 3줄 연속 출력.
```
UPBIT ETH/KRW price=3218000 tradedAt=2026-11-05T13:25:45.910Z
UPBIT ETH/KRW price=3218000 tradedAt=2026-11-05T13:25:45.910Z
UPBIT ETH/KRW price=3218000 tradedAt=2026-11-05T13:25:45.910Z
```

## 원인
중복 수신이 아니라, 큰 주문 하나가 여러 매도 주문과 같은 밀리초에 나눠 체결된 것. eventId를 로그에 함께 찍어 세 건의 `sequential_id`가 모두 다른 것을 확인했다.

## 해결
eventId를 체결 시각이 아닌 거래소 체결 고유 번호로 생성 (`UPBIT-{sequential_id}`).

## 배운 점
- 시각 기반 ID로 중복을 판단했다면 정상 체결 2건이 버려졌을 것
- "같아 보인다"와 "같다"를 구분할 수 있는 원천 식별자를 찾아야 한다
