package com.uni.coinpremium.collector;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * collector-service 헥사고날 구조 규칙
 */
@AnalyzeClasses(packages = "com.uni.coinpremium.collector", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule applicationDoesNotKnowAdapters = noClasses()
            .that().resideInAPackage("..collector.application..")
            .should().dependOnClassesThat().resideInAPackage("..collector.adapter..")
            .because("수집 규칙은 HTTP·Kafka 구현과 분리되어야 한다");

    @ArchTest
    static final ArchRule inboundDoesNotCallOutbound = noClasses()
            .that().resideInAPackage("..collector.adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..collector.adapter.out..")
            .because("WebSocket 수신은 발행 포트만 알고, Kafka 구현은 몰라야 한다");
}