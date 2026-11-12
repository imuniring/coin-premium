# 011. jOOQ 플러그인이 "JVM 21 이상 필요"로 실패

- 날짜: 2026-11-11
- 관련: [ADR 0005](../adr/0005-build-setup.md)

## 증상
```
Could not resolve org.jooq:jooq-codegen-gradle:3.21.9.
> Dependency requires at least JVM runtime version 21. This build uses a Java 17 JVM.
```
루트 `build.gradle.kts`에 툴체인 21을 설정했는데도 발생. 랩탑(기본 Java 21)에서는 문제없었다.

## 원인
Java가 쓰이는 곳이 두 군데다.

| 용도 | 결정하는 곳 | 해당 PC |
| --- | --- | --- |
| 우리 코드 컴파일·실행 | `toolchain` 설정 | 21 |
| Gradle 자체 실행 (플러그인 포함) | `JAVA_HOME` | 17 (`temurin-17`, 회사 프로젝트용) |

jOOQ 코드 생성 플러그인은 Gradle 안에서 실행되므로 툴체인이 아니라 Gradle 실행 JVM을 따른다.

## 해결
전역 `JAVA_HOME`은 유지하고 이 프로젝트의 Gradle만 21로 실행되게 고정했다.
```properties
# gradle/gradle-daemon-jvm.properties
toolchainVersion=21
```
```bash
./gradlew --stop
```
Gradle이 설치된 JDK(`/usr/lib/jvm/java-21-openjdk-amd64`)를 찾아 데몬을 21로 띄운다. 이 파일은 비밀값이 없어 커밋한다.

## 배운 점
툴체인(코드 컴파일)과 Gradle 실행 JVM은 다르다. 빌드 플러그인의 JVM 요구사항은 후자를 기준으로 본다.
