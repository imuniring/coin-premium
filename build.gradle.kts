plugins {
    // Spring Boot 플러그인은 "선언만" 하고 루트엔 적용 안 함
    // → 서비스 모듈에서 alias(libs.plugins.spring.boot)로 버전 없이 적용
    alias(libs.plugins.spring.boot) apply false
}

// 모든 하위 모듈(event-contract, collector, premium)에 공통 적용
subprojects {
    apply(plugin = "java")

    group = "com.uni"
    version = "0.0.1-SNAPSHOT"

    // 모든 모듈 Java 21
    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        // 버전은 Spring Boot BOM에 위임 (dependency-management 플러그인 대체)
        "implementation"(platform(rootProject.libs.spring.boot.bom))
        "testImplementation"(platform(rootProject.libs.spring.boot.bom))

        // 공통 테스트 기반
        "testImplementation"(rootProject.libs.junit.jupiter)
        "testRuntimeOnly"(rootProject.libs.junit.launcher)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}