# 0004. 스키마 관리로 Flyway 선택

- 상태: 채택 (2026-11-11 premium-service 적용)
- 날짜: 2026-11-05

## 맥락
스키마 변경 이력을 코드로 관리하고, jOOQ 코드 생성의 원천으로 쓰고 싶다.

## 선택지
- A: Flyway (버전별 SQL 파일을 순서대로 적용)
- B: Atlas (원하는 최종 스키마를 선언, 차이를 계산)

## 결정
A. Flyway(`spring-boot-starter-flyway` + `flyway-mysql`)를 사용하고, Atlas는 마이그레이션 lint 용도로 CI에서 선택적으로 검토한다.

## 이유
- Spring Boot 자동 지원, 앱 기동 시 미적용 버전만 실행
- SQL 파일 하나가 DB 스키마(Flyway)와 Java 코드(jOOQ DDLDatabase)의 공통 원본이 된다
- `flyway_schema_history`에 적용 이력과 checksum을 남겨, 이미 적용된 파일이 수정되면 기동을 막는다

## 운영 규칙
- 이미 적용된 `V{n}__*.sql`은 수정하지 않는다. 변경은 새 버전 파일로 추가한다
- 로컬에서 아직 공유되지 않은 마이그레이션을 고쳐야 하면 `docker compose down -v`로 DB를 초기화한다
- 배포 중 구·신 버전 앱이 공존할 수 있으므로 호환되지 않는 변경은 expand-contract로 나눈다 (6단계)

## 검증
- 첫 기동: `Successfully applied 1 migration ... now at version v1`
- 재기동: `Schema is up to date. No migration necessary.`
- Testcontainers 통합 테스트에서 매번 빈 MariaDB에 마이그레이션 적용

## 트레이드오프
- 위험한 변경(컬럼 삭제, 긴 락) 자동 감지가 없음 → Atlas lint로 보완 검토
