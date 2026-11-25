# 0018. 외부 API 복원력: Resilience4j 재시도·서킷 브레이커

- 상태: 채택
- 날짜: 2026-11-25
- 관련: #48, #50, ADR 0010

## 맥락
- 1단계에서는 `ExchangeRateCollector`(application)가 반복문과 `Thread.sleep`으로 직접 재시도했다 (ADR 0010).
- 재시도 규칙(횟수, 대기, 대상 예외)이 업무 흐름 코드에 섞여 있었다.
- 환율 API가 오래 죽어 있어도 30분마다 매번 3번씩 호출했고, "지금 죽어 있다"는 상태를 서비스가 알지 못했다.
- 2단계 완료 기준("환율 API 다운에도 조회 API 정상")과 5단계 알림("서킷 열림 → Discord")에 서킷 상태가 필요하다.

## 선택지
- A: 직접 구현 유지
- B: Spring Framework 7 내장 복원력 (`@Retryable`, `@ConcurrencyLimit`)
- C: Resilience4j (`resilience4j-spring-boot4`)

## 결정
C. Resilience4j로 재시도와 서킷 브레이커를 적용하고, 외부 호출을 담당하는 adapter(`KoreaEximClient`)에 둔다.

## 이유
- B는 재시도·동시성 제한은 있지만 서킷 브레이커가 없다.
- 재시도까지 Resilience4j로 통일해 설정(yml)과 이벤트·메트릭 이름을 한 곳에서 관리한다.
- 재시도·서킷 브레이커는 "외부 API를 어떻게 호출할까"의 문제라 adapter 책임이다. application에는 "최근 영업일 환율을 찾아 발행한다"는 업무 흐름만 남는다.
- 나중에 환율 출처가 늘어나도(테더 기준 김프 등) 어댑터마다 자기 규칙을 가질 수 있다.

## 구성
- 의존성: `resilience4j-spring-boot4` 2.4.0, `spring-boot-starter-aspectj` (Boot 4에서 어노테이션 AOP 지원이 이 스타터로 이동)
- 적용: `KoreaEximClient.fetchUsdKrw`에 `@Retry(name = "koreaexim")`, `@CircuitBreaker(name = "koreaexim")`
- 실행 순서: Resilience4j 기본값대로 `Retry ( CircuitBreaker ( 호출 ) )`. 서킷이 열리면 `CallNotPermittedException`이 나고 재시도 대상이 아니므로 즉시 끝난다.
- 대상 예외: `RetryableFailure` = `ExchangeRateFetchException` 중 `retryable=true`. 재시도(`retry-exception-predicate`)와 실패 집계(`record-failure-predicate`)에 같은 기준을 쓴다.
- 서킷 열림 처리: `fallbackMethod`로 `CallNotPermittedException`만 받아 `ExchangeRateFetchException(retryable=false)`으로 바꿔 던진다. application이 Resilience4j 예외를 모르게 한다.
- 이벤트 로그: `KoreaEximResilienceLogger`가 재시도(`attempt`, `delayMs`)와 서킷 상태 변경(`fromState`, `toState`)을 기록한다. 호출 스레드에서 실행되어 `job`, `jobId` MDC가 붙는다.

| 설정 | 값 | 근거 |
| --- | --- | --- |
| 재시도 | 최대 3번, 1초 → 2초 | 직접 구현 동작을 그대로 이전 (값까지 바꾸면 문제 원인 구분이 어려움) |
| 서킷 판단 | 최근 3번 호출(COUNT_BASED), 실패율 100% | 실행 1회(3번 시도)가 모두 실패하면 차단 |
| OPEN 유지 | 10분 | 스케줄 주기(30분)보다 짧아야 다음 실행에서 시험 호출(HALF_OPEN) |
| HALF_OPEN 시험 호출 | 1번 | 30분에 한 번 도는 작업이라 한 번이면 충분 |
| 실패 집계 | retryable=true만 | 4xx·인증 오류·인증서 오류는 서버 건강과 무관 |

## 검증
- `KoreaEximResilienceTest`: JDK `HttpServer`로 가짜 수출입은행 서버를 띄워 5xx 재시도 후 성공(3회), 4xx 즉시 포기(1회), 3회 실패 후 서킷 열림·4번째 호출 차단을 확인한다. 대기 시간만 10ms로 덮어쓰고 횟수·서킷 기준은 실제 yml 값을 쓴다.
- 수동: `--exchange.koreaexim.url=http://127.0.0.1:9/`로 기동해 1초·2초 재시도 후 `CLOSED → OPEN` 로그를 확인했다.
- 호환성: 2.4.0은 Boot 4.0.0 기준으로 빌드됐으며 Boot 4.1.2에서 기동·동작을 확인했다.

## 트레이드오프
- 어노테이션은 AOP 프록시로 동작한다. Spring 빈을 외부에서 호출할 때만 적용되고, 같은 클래스 안에서 부르거나 `new`로 만든 객체에는 적용되지 않는다.
- Spring은 모르는 yml 속성을 에러 없이 무시한다. 속성 이름 오타가 조용히 동작을 바꾸므로 설정 검증 테스트로 막는다 (트러블슈팅 017).
- 30분 주기 작업이라 서킷 브레이커의 호출 감소 효과는 작다 (장애 중 실행당 3번 → 1번). 주된 가치는 서비스가 장애 상태를 인지하는 것이다.
- 서킷 상태는 아직 로그로만 볼 수 있다 → Actuator·Micrometer로 노출 예정.
- 다시 볼 조건: 외부 API가 늘거나 호출 빈도가 높아질 때 TIME_BASED 창, 레이트 리미터, 타임 리미터 검토.
