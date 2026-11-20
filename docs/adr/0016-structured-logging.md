# 0016. 구조화 로깅: Spring Boot 내장 ECS 형식

- 상태: 채택
- 날짜: 2026-11-15
- 관련: #30

## 맥락
- 1단계 로그는 `[kafka]`, `[upbit]` 태그를 붙인 텍스트 로그였다.
- 5단계에 Loki·Grafana를 붙이면 서비스·거래소·상태 코드 같은 필드 단위 검색과 집계가 필요하다.
- 텍스트 로그는 문장 모양에 의존해 검색해야 하고, 여러 줄짜리 스택 트레이스가 수집 도구에서 쪼개지기 쉽다.

## 선택지
- A: Spring Boot 내장 구조화 로깅 (`logging.structured.format.console`)
- B: `logstash-logback-encoder` + logback XML 설정
- C: 텍스트 로그 유지, 수집 단계에서 정규식 파싱

## 결정
A. Spring Boot 내장 구조화 로깅을 ECS(Elastic Common Schema) 형식으로, `json` profile에서만 켠다.

## 이유
- 추가 의존성과 logback XML 없이 `application-json.yml` 설정만으로 적용된다.
- ECS는 `service.name`, `log.level`, `error.stack_trace`처럼 필드 이름이 표준으로 정해져 있어, 서비스가 늘어나도 검색 쿼리를 통일할 수 있다.
- MDC 값과 SLF4J 2 fluent API의 `addKeyValue` 값이 자동으로 JSON 필드가 된다 (Boot 4.1.2에서 확인).
- 스택 트레이스가 한 줄 JSON 안에 들어가 로그가 쪼개지지 않는다.
- 로컬 개발은 텍스트 로그가 읽기 쉬우므로 profile로 분리했다.

## 구성
- `logging.structured.format.console: ecs`
- `service.name`: `${spring.application.name}`
- `service.node.name`: `${HOSTNAME:local}` (컨테이너·Pod 단위 인스턴스 구분)
- `service.environment`: `local` (K8s 단계에서 덮어씀)

## 트레이드오프
- 필드 이름이 매 줄 반복되어 로그 크기가 커진다.
- 콘솔에서 사람이 바로 읽기 어렵다 → 로컬은 텍스트, 컨테이너·K8s에서만 `json` profile 사용.
- ECS가 정하지 않은 필드(`requestId`, `exchange` 등)는 최상위에 그대로 붙는다. 필드 이름 규칙은 로그 태그를 필드로 옮기는 작업에서 정한다.
- 다시 볼 조건: 로그 양이 비용 문제가 되거나, 수집 도구가 다른 형식(Logstash, GELF)을 요구할 때.
