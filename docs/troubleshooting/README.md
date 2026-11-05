# 트러블슈팅

| 번호 | 문제 | 분류 |
| --- | --- | --- |
| [001](001-intellij-kotlin-dsl.md) | build.gradle.kts 전체 빨간 줄 (KotlinBuildScript 접근 불가) | 빌드·IDE |
| [002](002-docker-port-conflict.md) | Docker Compose 3306·9092 포트 충돌 | 로컬 인프라 |
| [003](003-same-millisecond-trades.md) | 같은 시각·같은 가격 체결이 중복처럼 보임 | 도메인 |
| [004](004-clock-skew.md) | 체결 시각이 수신 시각보다 미래 | 분산 시스템·시간 |
| [005](005-zombie-connection.md) | 네트워크 단절 시 WebSocket이 멈춘 채 복구 안 됨 | 안정성 |
| [006](006-koreaexim-api-traps.md) | 수출입은행 API 도메인 변경·쉼표 문자열 | 외부 API |

## 템플릿

```markdown
# 00N. 제목

- 날짜: YYYY-MM-DD
- 관련: PR, ADR

## 증상
## 원인
## 해결
## 배운 점
```
