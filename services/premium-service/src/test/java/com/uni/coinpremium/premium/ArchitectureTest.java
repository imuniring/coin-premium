package com.uni.coinpremium.premium;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * premium-service 헥사고날 구조 규칙
 * - 테스트 코드는 검사 대상에서 제외 (테스트는 어댑터를 직접 써도 됨)
 */
@AnalyzeClasses(packages = "com.uni.coinpremium.premium", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    // 1. 도메인은 순수 Java: 프레임워크·DB·메시징·메시지 형식에 의존 금지
    @ArchTest
    static final ArchRule domainIsPure = noClasses()
            .that().resideInAPackage("..premium.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "org.jooq..",
                    "org.apache.kafka..",
                    "tools.jackson..",
                    "com.uni.coinpremium.premium.jooq..",   // jOOQ 생성 코드
                    "com.uni.coinpremium.contract..")       // Kafka 메시지 형식
            .because("도메인은 바깥 기술이 바뀌어도 영향받지 않아야 한다");

    // 2. 애플리케이션은 어댑터를 모름 (포트 인터페이스만 사용)
    @ArchTest
    static final ArchRule applicationDoesNotKnowAdapters = noClasses()
            .that().resideInAPackage("..premium.application..")
            .should().dependOnClassesThat().resideInAnyPackage("..premium.adapter..")
            .because("구현체(메모리→Redis 등)를 바꿔도 서비스 코드는 그대로여야 한다");

    // 3. 들어오는 어댑터는 나가는 어댑터를 직접 부르지 않음 (반드시 포트·서비스 경유)
    @ArchTest
    static final ArchRule inboundDoesNotCallOutbound = noClasses()
            .that().resideInAPackage("..premium.adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..premium.adapter.out..")
            .because("Controller·Listener가 저장소 구현체를 직접 쓰면 계층이 무너진다");
}
