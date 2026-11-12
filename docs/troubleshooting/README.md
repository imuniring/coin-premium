# 트러블슈팅

| 번호 | 문제 | 분류 |
| --- | --- | --- |
| [001](001-intellij-kotlin-dsl.md) | build.gradle.kts 전체 빨간 줄 (KotlinBuildScript 접근 불가) | 빌드·IDE |
| [002](002-docker-port-conflict.md) | Docker Compose 3306·9092 포트 충돌 | 로컬 인프라 |
| [003](003-same-millisecond-trades.md) | 같은 시각·같은 가격 체결이 중복처럼 보임 | 도메인 |
| [004](004-clock-skew.md) | 체결 시각이 수신 시각보다 미래 | 분산 시스템·시간 |
| [005](005-zombie-connection.md) | 네트워크 단절 시 WebSocket이 멈춘 채 복구 안 됨 | 안정성 |
| [006](006-koreaexim-api-traps.md) | 수출입은행 API 도메인 변경·쉼표 문자열 | 외부 API |
| [007](007-port-8081-in-use.md) | premium-service 기동 시 8081 포트 사용 중 | 로컬 환경 |
| [008](008-new-pc-wsl-setup.md) | 새 PC(WSL)에서 실행이 안 됨 | 개발 환경 |
| [009](009-class-name-collision.md) | 같은 이름의 클래스를 잘못 import | IDE·코드 |
| [010](010-bigdecimal-double-constructor.md) | `new BigDecimal(double)`로 오차 유입 | 금액 정밀도 |
| [011](011-gradle-jvm-vs-toolchain.md) | jOOQ 플러그인이 "JVM 21 이상 필요"로 실패 | 빌드 |
| [012](012-fetchone-test-isolation.md) | 통합 테스트의 fetchOne 유일성과 테스트 독립성 | 테스트 |

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
