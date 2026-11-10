plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":common:event-contract"))
    implementation(libs.spring.webmvc)    // 조회 API 제공 (collector와 다르게 웹 서버 필요)
    implementation(libs.spring.kafka)
    implementation(libs.spring.jackson)

    testImplementation(libs.spring.boot.test)
}