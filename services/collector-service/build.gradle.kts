plugins {
    alias(libs.plugins.spring.boot)   // 루트에서 선언만 해둔 걸 여기서 실제 적용
}

dependencies {
    implementation(project(":common:event-contract"))   // 이벤트 계약 모듈
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.jackson)

    testImplementation(libs.spring.boot.test)
}