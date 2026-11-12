package com.uni.coinpremium.premium.adapter.out.persistence;

import com.uni.coinpremium.premium.TestcontainersConfig;
import com.uni.coinpremium.premium.domain.HistoryInterval;
import com.uni.coinpremium.premium.domain.PremiumCandle;
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
import java.time.ZoneOffset;
import java.util.List;

import static com.uni.coinpremium.premium.jooq.Tables.PREMIUM_SNAPSHOT;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfig.class)
class JooqPremiumHistoryRepositoryTest {

    @Autowired
    JooqPremiumHistoryRepository repository;

    @Autowired
    DSLContext dsl;

    @BeforeEach
    void cleanUp() {
        dsl.deleteFrom(PREMIUM_SNAPSHOT).execute();
    }

    // 김프율만 의미 있게 넣고 나머지 값은 고정
    private void insert(String capturedAtUtc, String rate) {
        LocalDateTime at = LocalDateTime.ofInstant(Instant.parse(capturedAtUtc), ZoneOffset.UTC);
        dsl.insertInto(PREMIUM_SNAPSHOT)
                .set(PREMIUM_SNAPSHOT.SYMBOL, "BTC")
                .set(PREMIUM_SNAPSHOT.PREMIUM_RATE, new BigDecimal(rate))
                .set(PREMIUM_SNAPSHOT.DOMESTIC_PRICE, BigDecimal.ONE)
                .set(PREMIUM_SNAPSHOT.FOREIGN_PRICE, BigDecimal.ONE)
                .set(PREMIUM_SNAPSHOT.EXCHANGE_RATE, BigDecimal.ONE)
                .set(PREMIUM_SNAPSHOT.DOMESTIC_TRADED_AT, at)
                .set(PREMIUM_SNAPSHOT.FOREIGN_TRADED_AT, at)
                .set(PREMIUM_SNAPSHOT.FX_PUBLISHED_AT, at)
                .set(PREMIUM_SNAPSHOT.CAPTURED_AT, at)
                .execute();
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but " + actual);
    }

    @Test
    @DisplayName("1시간 캔들: 첫 값=시가, 마지막 값=종가, 최고·최저·평균·개수")
    void hourlyCandle() {
        insert("2026-11-11T05:00:00Z", "0.62");
        insert("2026-11-11T05:01:00Z", "0.81");
        insert("2026-11-11T05:02:00Z", "0.58");
        insert("2026-11-11T05:03:00Z", "0.70");
        insert("2026-11-11T06:00:00Z", "0.90");   // 다음 시간

        List<PremiumCandle> candles = repository.findCandles("BTC", HistoryInterval.HOUR,
                Instant.parse("2026-11-11T05:00:00Z"), Instant.parse("2026-11-11T07:00:00Z"));

        assertEquals(2, candles.size());
        PremiumCandle first = candles.get(0);
        assertEquals(Instant.parse("2026-11-11T05:00:00Z"), first.time());
        assertDecimal("0.62", first.open());
        assertDecimal("0.81", first.high());
        assertDecimal("0.58", first.low());
        assertDecimal("0.70", first.close());
        assertDecimal("0.6775", first.avg());   // (0.62 + 0.81 + 0.58 + 0.70) / 4
        assertEquals(4, first.count());
    }

    @Test
    @DisplayName("1일 캔들은 KST 자정(UTC 15:00)을 기준으로 나뉜다")
    void dailyCandleUsesKstMidnight() {
        insert("2026-11-11T14:59:00Z", "0.50");   // KST 11/11 23:59
        insert("2026-11-11T15:00:00Z", "0.90");   // KST 11/12 00:00

        List<PremiumCandle> candles = repository.findCandles("BTC", HistoryInterval.DAY,
                Instant.parse("2026-11-10T15:00:00Z"), Instant.parse("2026-11-12T15:00:00Z"));

        assertEquals(2, candles.size());
        assertEquals(Instant.parse("2026-11-10T15:00:00Z"), candles.get(0).time());   // KST 11/11 시작
        assertEquals(Instant.parse("2026-11-11T15:00:00Z"), candles.get(1).time());   // KST 11/12 시작
    }
}