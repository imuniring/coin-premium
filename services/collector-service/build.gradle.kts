plugins {
    alias(libs.plugins.spring.boot)   // 루트에서 선언만 해둔 걸 여기서 실제 적용
}

dependencies {
    implementation(project(":common:event-contract"))   // 이벤트 계약 모듈
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.jackson)
    implementation(libs.spring.kafka)   // 카탈로그에 이미 있음 (spring-boot-starter-kafka)
    implementation(libs.spring.data.redis)   // 리더 선출 열쇠 저장소 (Valkey)

    implementation(libs.spring.webmvc)     // 관리용 웹 서버 (API 없음, Actuator 창구만)
    implementation(libs.spring.actuator)   // 서킷 상태·로그 레벨·메트릭 조회

    implementation(libs.resilience4j.spring.boot)   // 환율 API 재시도·서킷 브레이커
    implementation(libs.spring.aspectj)             // 어노테이션 기반 적용에 필요한 AOP

    testImplementation(libs.spring.boot.test)
    testImplementation(libs.testcontainers.junit)   // 리더 선출 테스트용 Valkey 컨테이너
    testImplementation(libs.archunit)
}