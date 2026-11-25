# 0019. collector Actuator 도입: 관리용 웹 서버, 서킷 상태는 health 제외

- 상태: 채택
- 날짜: 2026-11-25
- 관련: #54, ADR 0018

## 맥락
- 서킷 브레이커 상태가 전환 시점 로그로만 남아, 현재 상태를 알려면 로그를 뒤져야 했다 (ADR 0018).
- collector는 API가 없어 웹 서버 없이 `spring.main.keep-alive: true`로 실행되고 있었다. HTTP 창구가 없으면 Actuator를 열 곳이 없다.
- 6단계 K8s probe와 5단계 Prometheus 수집도 HTTP 창구를 전제로 한다.

## 선택지
- A: `spring-boot-starter-webmvc`로 관리용 웹 서버를 띄우고 Actuator만 노출
- B: WebFlux(Netty) 경량 서버
- C: 웹 서버 없이 유지 (JMX 등)

## 결정
A. collector에 관리용 웹 서버(8082)를 띄우고, API는 만들지 않고 Actuator 창구만 연다.

## 이유
- premium과 같은 방식이라 일관된다.
- B는 리액티브 코드가 없는데 기술 스택만 하나 늘어난다.
- C는 K8s probe·Prometheus 수집이 HTTP 기반이라 6단계에서 막힌다.
- API가 없으므로 collector가 "수집 전용 서비스"라는 성격은 유지된다.

## 구성
- 포트 8082 (premium 8081과 분리)
- 노출: `health`, `circuitbreakers`, `retries`, `loggers`, `metrics`
- `prometheus`는 수집기를 붙이는 5단계에 노출
- 웹 서버가 앱을 유지하므로 `spring.main.keep-alive` 제거

## 서킷 상태를 health에 넣지 않은 이유
Resilience4j는 서킷 상태를 health에 포함하는 옵션이 있다. 포함하면 서킷이 OPEN일 때 health가 DOWN이 될 수 있다.
6단계에서 K8s는 health(liveness)가 DOWN이면 Pod를 재시작하는데, 서킷이 열린 원인은 외부 API(수출입은행) 장애이지 collector 고장이 아니다.
재시작해도 외부 API는 그대로이고, 그동안 WebSocket 시세 수집까지 끊긴다.
→ health는 collector 자체 상태만, 서킷 상태는 `/actuator/circuitbreakers`로 따로 조회한다.

## 검증
- 정상: `health` UP, `circuitbreakers` koreaexim CLOSED
- `--exchange.koreaexim.url=http://127.0.0.1:9/`로 기동: `circuitbreakers` OPEN(failedCalls 3, failureRate 100%), `health`는 UP 유지
- `metrics/resilience4j.retry.calls?tag=kind:failed_with_retry` = 1

## 트레이드오프
- `show-details: always`는 응답에 서버 경로(사용자 홈 디렉터리 등)가 노출된다. 로컬 확인용이며 외부 노출 시 제한한다.
- `loggers`는 로그 레벨을 바꿀 수 있는 쓰기 창구다. 외부에 열지 않거나 인증을 건다 (gateway·6단계).
- 다시 볼 조건: 6단계 K8s 배포 시 관리 포트 분리(`management.server.port`)와 노출 범위 재검토.
