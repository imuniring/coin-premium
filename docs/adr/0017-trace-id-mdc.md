# 0017. 추적 ID(MDC) 설계: 입구별 키와 비동기 경계 전파

- 상태: 채택
- 날짜: 2026-11-15
- 관련: #32, #34, #36, #39, #41, ADR 0016

## 맥락
- 동시 요청과 메시지의 로그가 섞여, 요청·실행·메시지·연결 단위로 원인을 추적할 수 없었다.
- MDC는 스레드별 저장소(ThreadLocal)라 HTTP 필터에서 넣은 값이 스케줄러, Kafka 리스너, WebSocket 콜백, 비동기 발행 콜백에는 전달되지 않는다.
- 5단계에 OpenTelemetry를 붙이면 `traceId`, `spanId`가 MDC에 자동으로 들어간다.

## 선택지
- A: 모든 입구에서 같은 키(`traceId` 또는 `requestId`) 사용
- B: 입구 성격에 맞는 키를 따로 사용
- C: 지금 Micrometer Tracing·OpenTelemetry 도입

## 결정
B. 입구마다 의미가 드러나는 키를 쓰고, 비동기 경계는 직접 전파한다. OpenTelemetry는 계획대로 5단계에 도입한다.

| 입구 | MDC 키 | 생성 시점 |
| --- | --- | --- |
| HTTP 요청 | `requestId` | 필터. 유효한 `X-Request-Id` 헤더는 재사용, 없으면 UUID. 응답 헤더로 반환 |
| 스케줄러 | `job`, `jobId` | 실행 1회마다 |
| Kafka 리스너 | `kafkaRecord`, `eventId` | `kafkaRecord`(topic-partition@offset)는 파싱 전, `eventId`는 파싱 후 |
| WebSocket | `exchange`, `connectionId` | 연결 시도마다, 콜백 입구마다 넣고 정리 |
| 비동기 발행 콜백 | 호출 시점 MDC 복사본 | 발행 직전 복사, 콜백에서 복원 |

## 이유
- 키 이름만으로 로그 출처(API 요청·스케줄러·메시지·연결)가 구분된다.
- `traceId`를 쓰지 않아 OpenTelemetry 도입 시 이름 충돌이 없다.
- `kafkaRecord`는 파싱이 실패해도 원본 메시지 위치를 남긴다.
- `eventId`로 collector 발행 로그와 premium 처리 로그를 서비스 경계를 넘어 이어 볼 수 있다.
- C는 "문제 → 기술" 원칙상 분산 추적이 필요해지는 5단계가 적절하다. 직접 구현으로 MDC 동작 원리를 먼저 이해한다.

## 구현 원칙
- 정리는 필수: Kafka 리스너·WebSocket 콜백 스레드는 재사용되므로 값이 다음 작업에 남으면 로그가 오염된다.
- `MDC.clear()` 대신 내가 넣은 키만 제거하거나 이전 상태로 복원한다 (다른 코드나 OpenTelemetry가 넣은 값 보존).
- 외부 입력(`X-Request-Id`)은 영문·숫자·하이픈 64자 이내만 허용한다 (로그 인젝션 방지).
- catch 로그에도 값이 필요하면 try-with-resources 대신 `put` + `finally`를 쓰거나 MDC를 바깥에서 연다 (트러블슈팅 015).
- 요청 완료 액세스 로그(메서드, 경로, 상태, 처리 시간)를 함께 남긴다. 쿼리 스트링과 본문은 남기지 않는다.

## 트레이드오프
- 검색할 키가 여러 개다.
- 비동기 경계마다 직접 전파해야 하고, 새 비동기 코드를 추가할 때 빠뜨릴 수 있다.
- Kafka 클라이언트 지연 생성 로그처럼, 같은 스레드에서 일어난 무관한 로그에도 MDC가 붙을 수 있다.
- 비동기 발행 콜백 경로(버퍼 시간 초과)는 로컬에서 재현하지 못했다. Toxiproxy 지연 주입 테스트에서 검증한다.
- 다시 볼 조건: 5단계 OpenTelemetry 도입 시 `requestId`를 `traceId`로 대체할지, 함께 쓸지 결정한다.
