# 0008. 결정적 eventId 설계

- 상태: 채택
- 날짜: 2026-11-05

## 맥락
재연결이나 재조회 시 같은 데이터가 다시 들어올 수 있다. 랜덤 UUID를 쓰면 같은 체결에 매번 다른 ID가 붙어 소비자가 중복을 판단할 수 없다.

## 결정
같은 원천 데이터는 항상 같은 ID가 되도록 원천의 고유값으로 eventId를 만든다.

| 이벤트 | eventId | 근거 |
| --- | --- | --- |
| Upbit 체결 | `UPBIT-{sequential_id}` | Upbit이 체결마다 부여하는 고유 번호 |
| Binance 체결 | `BINANCE-{symbol}-{tradeId}` | 체결 ID가 심볼별로만 유일해서 심볼 포함 |
| 환율 | `KOREAEXIM-USD-KRW-{yyyyMMdd}` | 하루 1회 고시 |

## 이유
- 소비자 멱등성(2단계)의 기반
- 환율은 30분마다 조회해도 같은 날짜면 같은 ID라 collector에서도 중복 발행 생략 가능

## 검토한 대안
- 체결 시각 기반 ID: 같은 밀리초에 여러 건 체결되는 경우가 실제로 있어 기각 ([트러블슈팅 003](../troubleshooting/003-same-millisecond-trades.md))
- 랜덤 UUID: 중복 판단 불가로 기각 (`TickerEvent.of()`는 테스트·임시용으로만 유지)

## 트레이드오프
- Upbit `sequential_id`의 유일성 범위(전체 / 마켓별)는 공식 문서로 재확인 필요. 마켓별이라면 Binance처럼 마켓 코드를 포함하도록 변경
