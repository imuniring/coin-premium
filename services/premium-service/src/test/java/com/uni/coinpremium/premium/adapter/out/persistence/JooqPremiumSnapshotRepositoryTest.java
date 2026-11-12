package com.uni.coinpremium.premium.adapter.out.persistence;

import com.uni.coinpremium.premium.TestcontainersConfig;
import com.uni.coinpremium.premium.application.service.PremiumSnapshot;
import com.uni.coinpremium.premium.domain.FxRate;
import com.uni.coinpremium.premium.domain.KimchiPremium;
import com.uni.coinpremium.premium.domain.PriceQuote;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import static com.uni.coinpremium.premium.jooq.Tables.PREMIUM_SNAPSHOT;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfig.class)
class JooqPremiumSnapshotRepositoryTest {

     @Autowired
    JooqPremiumSnapshotRepository repository;

     @Autowired
    DSLContext dsl;

    private static final Instant NOW = Instant.parse("2026-11-11T05:01:00Z");

    // 테스트마다 테이블을 비워서 다른 테스트 데이터와 섞이지 않게
    @BeforeEach
    void cleanUp() {
        dsl.deleteFrom(PREMIUM_SNAPSHOT).execute();
    }


    private PremiumSnapshot snapshot(String symbol) {
        var domestic = new PriceQuote("UPBIT", symbol, new BigDecimal("10731100"), NOW);
        var foreign = new PriceQuote("BINANCE", symbol, new BigDecimal("79528.57"), NOW);
        var fx = new FxRate(new BigDecimal("1340.7"), NOW);
        var premium = KimchiPremium.calculate(symbol, domestic.price(), foreign.price(), fx.rate(), NOW);
        return new PremiumSnapshot(premium, domestic, foreign, fx);
    }

    @Test
    @DisplayName("같은 코인·같은 시점은 한 번만 저장된다")
    void ignoreDuplicate() {
        assertTrue(repository.save(snapshot("BTC"), NOW));  // 처음: 저장
        assertFalse(repository.save(snapshot("BTC"), NOW)); // 두 번째: 무시
    }

    @Test
    @DisplayName("시각은 UTC 기준으로 저장된다")
    void storeInUtc() {
        repository.save((snapshot("ETH")), NOW);

        // 유일 키(symbol + captured_at)로 조회 → 최대 1개 보장
        // UTC로 저장됐다면 05:01로 찾아져야 함 (KST로 저장됐다면 14:01이라 못 찾음)
        LocalDateTime stored = dsl.select(PREMIUM_SNAPSHOT.CAPTURED_AT)
                .from(PREMIUM_SNAPSHOT)
                .where(PREMIUM_SNAPSHOT.SYMBOL.eq("ETH")
                        .and(PREMIUM_SNAPSHOT.CAPTURED_AT.eq(LocalDateTime.of(2026, 11, 11, 5, 1))))
                .fetchOne(PREMIUM_SNAPSHOT.CAPTURED_AT);

        assertNotNull(stored);
    }
}