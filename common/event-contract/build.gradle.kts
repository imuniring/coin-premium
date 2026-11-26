plugins {
    `java-library`   // 다른 서비스가 가져다 쓰는 부품 모듈
}

// 실행 시 의존성 없음: Spring 없이 순수 Java record만 둔다 (테스트 전용 의존성만 추가)
dependencies {
    testImplementation(libs.assertj)
}