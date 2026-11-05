# ADR (Architecture Decision Record)

기술을 도입하거나 구조를 바꿀 때마다 "왜 그렇게 결정했는지"를 남긴다.

| 번호 | 제목 | 상태 |
| --- | --- | --- |
| [0001](0001-msa-monorepo.md) | MSA + 모노레포 구성 | 채택 |
| [0002](0002-pragmatic-hexagonal.md) | 서비스 내부 실용적 헥사고날 | 채택 |
| [0003](0003-jooq.md) | 쿼리 도구로 jOOQ 선택 | 채택 (도입 예정) |
| [0004](0004-flyway.md) | 스키마 관리로 Flyway 선택 | 채택 (도입 예정) |
| [0005](0005-build-setup.md) | Spring Boot 4 · Version Catalog · BOM platform | 채택 |
| [0006](0006-websocket-first.md) | 1단계부터 WebSocket 수집, JDK HttpClient 사용 | 채택 |
| [0007](0007-kafka-key-and-topic.md) | Kafka 도입, 메시지 키 = 심볼 | 채택 |
| [0008](0008-deterministic-event-id.md) | 결정적 eventId 설계 | 채택 |
| [0009](0009-websocket-reconnect.md) | WebSocket 재연결·좀비 연결 감지 | 채택 |
| [0010](0010-exchange-rate-collection.md) | 환율 수집 전략 | 채택 |

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
