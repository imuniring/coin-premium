# 0013. 테스트 전략: Testcontainers + ArchUnit

- 상태: 채택
- 날짜: 2026-11-11

## 맥락
- premium-service가 DB에 의존하게 되면서 `contextLoads`가 `docker compose`를 미리 띄워야만 통과했다
- 헥사고날 규칙을 말로만 지키고 있어 실수로 계층이 무너져도 알 수 없었다

## 결정
### Testcontainers
- 테스트 실행 시 MariaDB 11.8, Kafka 4.1.0 컨테이너를 자동으로 띄운다 (`TestcontainersConfig`)
- `@ServiceConnection`으로 접속 정보를 자동 연결
- 이미지 버전은 docker-compose와 동일하게 맞춘다
- Testcontainers 2.x 아티팩트·패키지 사용 (`testcontainers-mariadb`, `org.testcontainers.mariadb.MariaDBContainer`)
- datasource 접속 정보에 기본값을 두어 `.env`가 없는 CI에서도 기동

### 테스트 계층
| 종류 | 대상 | 방식 |
| --- | --- | --- |
| 단위 | 도메인, 서비스 규칙 | Spring 없이, 포트는 람다·메모리 구현으로 대체 |
| 통합 | jOOQ 저장, Flyway, 컨텍스트 | Testcontainers |
| 구조 | 패키지 의존 규칙 | ArchUnit |

### ArchUnit 규칙
- domain → Spring·jOOQ·Kafka·Jackson·contract 의존 금지 (premium)
- application → adapter 의존 금지 (collector, premium)
- adapter.in → adapter.out 직접 사용 금지 (collector, premium)
- 규칙마다 `because`로 이유를 남긴다

### 빌드 JVM
- `gradle/gradle-daemon-jvm.properties`에 `toolchainVersion=21` → JAVA_HOME이 17인 PC에서도 Gradle은 21로 실행

## 이유
- 개발 DB를 오염시키지 않고, 로컬·CI에서 같은 방식으로 실행
- 통합 테스트는 테스트마다 테이블을 비우고 유일 키로 조회해 실행 순서와 무관하게 동작
- 서비스 간 import 금지는 Gradle 모듈 의존성으로 이미 컴파일 단계에서 강제되므로 ArchUnit은 서비스 내부 계층에 집중
- 도메인이 프레임워크에 의존하면 버전 업그레이드(javax→jakarta, Jackson 2→3)나 Flink 전환 시 함께 수정해야 함

## 검증
- `docker compose stop` 상태에서 전체 테스트 통과
- `KimchiPremium`에 `@Component`를 임시 추가 → `domainIsPure` 실패 + 빈 생성 실패 확인 후 원복

## 트레이드오프
- 첫 실행은 이미지 다운로드로 수 분 소요, Docker 필수
- 통합 테스트는 단위 테스트보다 느리므로 핵심 저장 로직에만 사용
