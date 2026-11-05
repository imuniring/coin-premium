# 0005. Spring Boot 4 · Version Catalog · BOM platform

- 상태: 채택
- 날짜: 2026-11-05

## 맥락
멀티 모듈에서 라이브러리 버전을 한 곳에서 관리하고, 모듈마다 같은 공통 설정을 반복하고 싶지 않다.

## 결정
- Spring Boot 4.1.2, Java 21
- Gradle Kotlin DSL + `gradle/libs.versions.toml` (Version Catalog)
- `io.spring.dependency-management` 플러그인 대신 `platform(spring-boot-dependencies)`로 BOM 적용
- 루트 `subprojects {}`에서 Java 툴체인·저장소·테스트 공통 설정
- Gradle Wrapper 8.14.3 ([트러블슈팅 001](../troubleshooting/001-intellij-kotlin-dsl.md))

## 이유
- Spring Boot가 관리하는 라이브러리는 카탈로그에 버전을 적지 않아 Boot와 어긋날 일이 없음
- `platform()`은 Gradle 기본 기능이라 플러그인 하나가 줄어듦
- Boot 4는 기술별 스타터로 모듈화됨 (`spring-boot-starter-webmvc`, `spring-boot-starter-kafka` 등). 스타터 없이 라이브러리만 넣으면 자동 설정이 빠질 수 있어 스타터 사용을 원칙으로 함
- Boot 4는 Jackson 3 (`tools.jackson.*`)을 사용

## 트레이드오프
- `subprojects {}`는 Gradle이 장기적으로 권장하지 않는 방식 → 모듈이 늘면 컨벤션 플러그인(build-logic)으로 전환
- 최신 Gradle(9.x) 대신 IDE 호환 버전 사용 → IDE 업데이트 시 재검토
