# ADR (Architecture Decision Record)

기술을 도입하거나 구조를 바꿀 때마다 "왜 그렇게 결정했는지"를 남긴다.

| 번호 | 제목 | 상태 |
| --- | --- | --- |
| [0001](0001-msa-monorepo.md) | MSA + 모노레포 구성 | 채택 |
| [0002](0002-pragmatic-hexagonal.md) | 서비스 내부 실용적 헥사고날 | 채택 |
| [0003](0003-jooq.md) | 쿼리 도구로 jOOQ 선택 (DDLDatabase 코드 생성) | 채택 |
| [0004](0004-flyway.md) | 스키마 관리로 Flyway 선택 | 채택 |
| [0005](0005-build-setup.md) | Spring Boot 4 · Version Catalog · BOM platform | 채택 |
| [0006](0006-websocket-first.md) | 1단계부터 WebSocket 수집, JDK HttpClient 사용 | 채택 |
| [0007](0007-kafka-key-and-topic.md) | Kafka 도입, 메시지 키 = 심볼 | 채택 |
| [0008](0008-deterministic-event-id.md) | 결정적 eventId 설계 | 채택 |
| [0009](0009-websocket-reconnect.md) | WebSocket 재연결·좀비 연결 감지 | 채택 |
| [0010](0010-exchange-rate-collection.md) | 환율 수집 전략 | 채택 |
| [0011](0011-premium-query-design.md) | premium-service 조회 설계 (메모리 최신값 + 요청 시 계산) | 채택 |
| [0012](0012-premium-snapshot.md) | 1분 단위 김프 스냅샷 저장 | 채택 |
| [0013](0013-test-strategy.md) | 테스트 전략: Testcontainers + ArchUnit | 채택 |
| [0014](0014-premium-history-api.md) | 김프 추이 API (김프 캔들) | 채택 |
| [0015](0015-error-response.md) | 에러 응답 설계 (ProblemDetail + code) | 채택 |
| [0016](0016-structured-logging.md) | 구조화 로깅: Spring Boot 내장 ECS 형식 | 채택 |
| [0017](0017-trace-id-mdc.md) | 추적 ID(MDC) 설계: 입구별 키와 비동기 경계 전파 | 채택 |
| [0018](0018-resilience4j.md) | 외부 API 복원력: Resilience4j 재시도·서킷 브레이커 | 채택 |
| [0019](0019-collector-actuator.md) | collector Actuator 도입: 관리용 웹 서버, 서킷 상태는 health 제외 | 채택 |
| [0020](0020-ticker-buffered-publishing.md) | 시세 수신·발행 분리: 보관함과 전담 발행 스레드 | 채택 |
| [0021](0021-valkey.md) | 공유 저장소로 Valkey 선택 (Redis 호환, BSD-3) | 채택 |
| [0022](0022-premium-shared-market-data.md) | premium 다중 인스턴스: 최신 시세를 공유 저장소로 | 채택 |
| [0023](0023-scheduler-lock.md) | 스케줄러 분산 락: ShedLock + Valkey | 채택 |
| [0024](0024-collector-leader-election.md) | collector 다중 인스턴스: 리더 선출(active-standby) | 채택 |
| [0025](0025-coin-registry.md) | 코인 레지스트리: 지원 코인과 거래소 마켓 코드를 공통 계약으로 | 채택 |
| [0026](0026-quote-freshness.md) | 시세 신선도: 저장은 엄격하게, 보여 주기는 솔직하게 | 채택 |
| [0027](0027-snapshot-backfill.md) | 김프 스냅샷 공백 감지·보정 | 채택 |
| [0028](0028-fx-rate-history.md) | 환율 이력과 일자 전환 규칙 | 채택 |

## 템플릿

```markdown
# 000N. 제목

- 상태: 제안 / 채택 / 폐기 / 대체됨(000M)
- 날짜: YYYY-MM-DD

## 맥락
어떤 문제가 있었나

## 선택지
- A:
- B:

## 결정
무엇을 골랐나

## 이유
왜 골랐나

## 트레이드오프
무엇을 포기했나, 나중에 다시 볼 조건은
```
