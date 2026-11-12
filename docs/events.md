# 이벤트 명세

이벤트 계약은 `common/event-contract` 모듈에 정의되어 있으며, 토픽 이름은 `Topics` 상수를 사용합니다.

## 공통 규칙

- 소비자는 메시지를 문자열로 받아 직접 역직렬화하고, 실패한 메시지는 로그 후 건너뛴다 (poison pill 방지)

- 직렬화: JSON (타입 헤더 미사용)
- 금액: `BigDecimal` (JSON 숫자). `double` 사용 금지
- 시간: `Instant`, ISO-8601 UTC 문자열 (예: `2026-11-05T13:35:53.664Z`)
- eventId: **결정적 ID**. 같은 원천 데이터는 항상 같은 ID → 소비자가 중복 판단
- 생성 시점 검증(Fail-Fast): null → `NullPointerException`, 잘못된 값 → `IllegalArgumentException`
- 토픽 이름에 버전(`.v1`)을 붙여, 호환되지 않는 변경 시 새 토픽으로 분리

## market.ticker.v1

| 항목 | 값 |
| --- | --- |
| 생산자 | collector-service |
| 소비자 | premium-service (group: premium-service) |
| 키 | 심볼 (예: `BTC`) |
| 파티션 | 3 |
| 전송 | 비동기 |
| 소비 시작 위치 | 기동 시 맨 끝부터 (밀린 과거 시세는 불필요) |

같은 코인의 국내·해외 시세가 같은 파티션에 모이도록 키를 심볼로 지정합니다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| eventId | String | Upbit: `UPBIT-{sequential_id}`, Binance: `BINANCE-{symbol}-{tradeId}` |
| exchange | UPBIT / BINANCE | 거래소 |
| symbol | String | 정규화된 심볼 (`BTC`) |
| quote | KRW / USDT | 가격 기준 통화 |
| price | BigDecimal | 체결 가격 |
| tradedAt | Instant | 거래소 체결 시각 |
| receivedAt | Instant | collector 수신 시각 |

```json
{
  "eventId": "BINANCE-BTCUSDT-6822280927",
  "exchange": "BINANCE",
  "symbol": "BTC",
  "quote": "USDT",
  "price": 79528.57000000,
  "tradedAt": "2026-11-05T14:12:50.464Z",
  "receivedAt": "2026-11-05T14:12:50.457442Z"
}
```

> 거래소와 수신 서버 사이 시계 오차로 `tradedAt`이 `receivedAt`보다 늦을 수 있습니다. → [트러블슈팅 004](troubleshooting/004-clock-skew.md)

## market.exchange-rate.v1

| 항목 | 값 |
| --- | --- |
| 생산자 | collector-service |
| 소비자 | premium-service (group: premium-service) |
| 키 | `USD-KRW` |
| 파티션 | 1 |
| 전송 | 동기 (5초 타임아웃) |
| 소비 시작 위치 | 기동 시 처음부터 (메모리 저장소가 비므로 마지막 환율 복구) |

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| eventId | String | `KOREAEXIM-USD-KRW-{yyyyMMdd}` (고시일 기준) |
| base / target | USD / KRW | 통화쌍 |
| rate | BigDecimal | 매매기준율 |
| source | KOREAEXIM | 환율 출처 |
| publishedAt | Instant | 고시일 0시(KST)를 UTC로 표현 |
| receivedAt | Instant | collector 수신 시각 |

```json
{
  "eventId": "KOREAEXIM-USD-KRW-20261105",
  "base": "USD",
  "target": "KRW",
  "rate": 1340.7,
  "source": "KOREAEXIM",
  "publishedAt": "2026-11-04T15:00:00Z",
  "receivedAt": "2026-11-05T15:11:43.787312Z"
}
```

> `publishedAt`의 `2026-11-04T15:00:00Z`는 KST 기준 `2026-11-05 00:00`입니다.
