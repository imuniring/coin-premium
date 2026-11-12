plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.jooq.codegen)
}

dependencies {
    implementation(project(":common:event-contract"))
    implementation(libs.spring.webmvc)
    implementation(libs.spring.kafka)
    implementation(libs.spring.jackson)

    implementation(libs.spring.jooq)
    implementation(libs.spring.flyway)
    implementation(libs.flyway.mysql)
    runtimeOnly(libs.mariadb)

    jooqCodegen(libs.jooq.meta.extensions)

    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.kafka)

    testImplementation(libs.archunit)
}

// jOOQ 코드 생성: Flyway SQL 파일을 읽어서 Java 코드 생성 (DB 접속 불필요)
jooq {
    configuration {
        generator {
            database {
                name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                properties {
                    property {
                        key = "scripts"
                        value = "src/main/resources/db/migration/*.sql"   // Flyway 파일 위치
                    }
                    property {
                        key = "sort"
                        value = "flyway"                                   // V1, V2... 순서대로 적용
                    }
                    property {
                        key = "defaultNameCase"
                        value = "lower"                                    // 테이블·컬럼명 소문자 유지
                    }
                }
            }
            target {
                packageName = "com.uni.coinpremium.premium.jooq"
                directory = "build/generated-sources/jooq"
            }
        }
    }
}

// 생성된 코드를 컴파일 대상에 포함하고, 컴파일 전에 항상 코드 생성
sourceSets {
    main {
        java {
            srcDir("build/generated-sources/jooq")
        }
    }
}
tasks.named("compileJava") {
    dependsOn("jooqCodegen")
}