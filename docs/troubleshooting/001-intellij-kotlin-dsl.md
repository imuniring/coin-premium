# 001. build.gradle.kts 전체 빨간 줄 (KotlinBuildScript 접근 불가)

- 날짜: 2026-11-05
- 관련: [ADR 0005](../adr/0005-build-setup.md)

## 증상
루트 `build.gradle.kts`의 `plugins`, `subprojects` 같은 기본 키워드까지 전부 빨간 줄. 메시지:
`Cannot access script base class 'org.gradle.kotlin.dsl.KotlinBuildScript'`

## 원인
`./gradlew projects`는 `BUILD SUCCESSFUL` → 빌드 설정은 정상.
사용 중인 IntelliJ 버전이 Gradle 9.8.2의 Kotlin DSL을 지원하지 못했다. (라이선스 만료로 IDE 업데이트 불가)

## 해결
Gradle Wrapper를 8.14.3으로 변경. Spring Boot 4가 요구하는 최소 Gradle 버전 이상인지 확인 후 적용.
```bash
./gradlew wrapper --gradle-version 8.14.3
```

## 배운 점
- IDE 에러와 실제 빌드 에러는 `./gradlew`로 먼저 구분한다
- 팝업의 "Import class 'plugins'" 같은 자동 수정은 원인과 무관하므로 누르지 않는다
