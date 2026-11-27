package com.uni.coinpremium.premium.adapter.out.persistence;

import static com.uni.coinpremium.premium.jooq.Tables.FX_RATE_HISTORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.uni.coinpremium.premium.TestcontainersConfig;
import com.uni.coinpremium.premium.domain.FxRate;
import java.math.BigDecimal;
import java.time.Instant;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class JooqFxRateHistoryTest {

    @Autowired
    JooqFxRateHistory history;

    @Autowired
    DSLContext dsl;

    // 고시 기준 시각: 해당 날짜 00:00 KST = 전날 15:00 UTC
    private static final Instant THU = Instant.parse("2026-11-25T15:00:00Z");   // 11/26(목) 고시
    private static final Instant FRI = Instant.parse("2026-11-26T15:00:00Z");   // 11/27(금) 고시

    @BeforeEach
    void cleanUp() {
        dsl.deleteFrom(FX_RATE_HISTORY).execute();
    }

    @Test
    @DisplayName("같은 고시 시각은 한 번만 저장된다 (토픽 재소비 대비)")
    void ignoreDuplicate() {
        history.save(new FxRate(new BigDecimal("1330.8"), FRI), "KOREAEXIM-USD-KRW-20261127");
        history.save(new FxRate(new BigDecimal("1330.8"), FRI), "KOREAEXIM-USD-KRW-20261127");

        assertEquals(1, dsl.fetchCount(FX_RATE_HISTORY));
    }

    @Test
    @DisplayName("그 시각 이전에 고시된 것 중 가장 최근 환율을 찾는다")
    void findEffective() {
        history.save(new FxRate(new BigDecimal("1343.6"), THU), "KOREAEXIM-USD-KRW-20261126");
        history.save(new FxRate(new BigDecimal("1330.8"), FRI), "KOREAEXIM-USD-KRW-20261127");

        // 목요일 오후 → 목요일 환율
        assertEquals(new BigDecimal("1343.6000"),
                history.findLatestPublishedUpTo(Instant.parse("2026-11-26T05:00:00Z")).orElseThrow().rate());
        // 일요일 새벽 → 고시 없는 주말이라 금요일 환율
        assertEquals(new BigDecimal("1330.8000"),
                history.findLatestPublishedUpTo(Instant.parse("2026-11-28T18:00:00Z")).orElseThrow().rate());
    }

    @Test
    @DisplayName("그 시각 이전 환율이 하나도 없으면 비어 있다")
    void emptyBeforeFirst() {
        history.save(new FxRate(new BigDecimal("1330.8"), FRI), "KOREAEXIM-USD-KRW-20261127");

        assertTrue(history.findLatestPublishedUpTo(THU).isEmpty());
    }
}