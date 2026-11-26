# 0022. premium 다중 인스턴스: 최신 시세를 공유 저장소로

- 상태: 채택
- 날짜: 2026-11-26
- 관련: #61, ADR 0021, 성능 002

## 맥락
premium을 2대 띄우자 같은 컨슈머 그룹(`premium-service`)이 파티션을 나눠 가졌다.
최신 시세·환율은 인스턴스 메모리에 있었기 때문에

- 8081: 환율 파티션은 받았지만 BTC·ETH가 오는 시세 파티션을 못 받음 → `missing: [UPBIT, BINANCE]`
- 8083: BTC·ETH 시세는 받았지만 환율 파티션을 못 받음 → `missing: [FX_RATE]`

두 대 모두 살아 있고 collector도 정상인데 김프 조회가 100% 실패했다 (성능 002).
환율 메시지는 키가 `USD-KRW` 하나라 항상 같은 파티션으로 가므로, 이 문제는 인스턴스가 늘수록 반드시 생긴다.

## 선택지
- A: 인스턴스마다 다른 컨슈머 그룹을 써서 각자 모든 파티션을 받음
- B: 소비는 나눠서 하고, 최신값은 공유 저장소(Valkey)에 모아 모든 인스턴스가 읽음

## 결정
B. `MarketDataRepository` 포트의 구현을 메모리에서 Valkey(`ValkeyMarketDataRepository`)로 바꾼다.

## 이유
- 모든 인스턴스가 같은 값을 본다. A는 인스턴스마다 소비 진행 상황이 달라 값이 미세하게 다를 수 있다.
- 새로 뜬 인스턴스도 바로 응답한다. A는 시세가 들어올 때까지 비어 있다.
- 포트·어댑터 구조(ADR 0011) 덕분에 어댑터만 교체했다.
- A는 인프라 추가가 없어 단순하지만, Valkey는 락·리더 선출·캔들·요청 수 제한에서도 재사용한다 (ADR 0021).

## 구성
- 키: `quote:{거래소}:{심볼}` → Hash `{price, tradedAt(epoch ms)}`, `fx:USD-KRW` → Hash `{rate, publishedAt(epoch ms)}`
- "더 최신일 때만 교체"(기존 `isNewerThan`, 같은 시각이면 교체)를 Lua 스크립트 하나로 원자적으로 처리한다.
  읽고-비교하고-쓰기를 나누면 리밸런싱 중 두 인스턴스가 동시에 쓸 때 옛날 값이 덮어쓸 수 있다.
- 금액은 `toPlainString()`으로 저장해 지수 표기를 피한다.
- `InMemoryMarketDataRepository`는 서비스 단위 테스트용 가짜로 test에 옮겼다.

## 검증
- `ValkeyMarketDataRepositoryTest`(Testcontainers): 최신 교체, 순서 뒤바뀜, 같은 값 두 번, 환율, 빈 값
- 2대 실행: 4개 조회 모두 실패 → 모두 성공 (성능 002)

## 트레이드오프
- 조회마다 Valkey 왕복이 생긴다.
- Valkey 장애 시 조회·저장 모두 실패한다 → Toxiproxy 묶음에서 검증.
- 다시 볼 조건: 조회량이 많아져 Valkey 왕복이 병목이 되면 짧은 로컬 캐시 검토.
