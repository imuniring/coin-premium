package com.uni.coinpremium.premium.adapter.out.persistence;

import com.uni.coinpremium.premium.application.port.out.PremiumHistoryRepository;
import com.uni.coinpremium.premium.domain.HistoryInterval;
import com.uni.coinpremium.premium.domain.PremiumCandle;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.uni.coinpremium.premium.jooq.Tables.PREMIUM_SNAPSHOT;
import static org.jooq.impl.DSL.*;

/**
 * 김프 이력 집계 (시가·고가·저가·종가·평균·개수)
 */
@Repository
public class JooqPremiumHistoryRepository implements PremiumHistoryRepository {

    // 시간 단위 구간의 기준점 (UTC 정각)
    private static final LocalDateTime HOUR_ANCHOR = LocalDateTime.of(2000, 1, 1, 0, 0);
    // 일 단위 구간의 기준점: KST 자정 = UTC 15:00 → 이 시각부터 하루씩 자르면 KST 날짜 경계가 됨
    private static final LocalDateTime KST_DAY_ANCHOR = LocalDateTime.of(2000, 1, 1, 15, 0);

    private final DSLContext dsl;

    public JooqPremiumHistoryRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<PremiumCandle> findCandles(String symbol, HistoryInterval interval, Instant from, Instant to) {
        Field<LocalDateTime> bucket = bucketOf(interval);

        // 1단계: 각 행에 "어느 구간인지"와 "그 구간의 첫·마지막 김프"를 붙임 (윈도우 함수)
        Table<?> t = dsl.select(
                        bucket.as("bucket"),
                        PREMIUM_SNAPSHOT.PREMIUM_RATE.as("rate"),
                        firstValue(PREMIUM_SNAPSHOT.PREMIUM_RATE)
                                .over(partitionBy(bucket).orderBy(PREMIUM_SNAPSHOT.CAPTURED_AT.asc()))
                                .as("open_rate"),
                        firstValue(PREMIUM_SNAPSHOT.PREMIUM_RATE)
                                .over(partitionBy(bucket).orderBy(PREMIUM_SNAPSHOT.CAPTURED_AT.desc()))
                                .as("close_rate"))
                .from(PREMIUM_SNAPSHOT)
                .where(PREMIUM_SNAPSHOT.SYMBOL.eq(symbol)
                        .and(PREMIUM_SNAPSHOT.CAPTURED_AT.ge(utc(from)))
                        .and(PREMIUM_SNAPSHOT.CAPTURED_AT.lt(utc(to))))
                .asTable("t");

        Field<LocalDateTime> tBucket = t.field("bucket", LocalDateTime.class);
        Field<BigDecimal> rate = t.field("rate", BigDecimal.class);
        Field<BigDecimal> open = t.field("open_rate", BigDecimal.class);
        Field<BigDecimal> close = t.field("close_rate", BigDecimal.class);

        // 2단계: 구간별로 묶어서 캔들 계산
        return dsl.select(tBucket, min(open), max(rate), min(rate), min(close), avg(rate), count())
                .from(t)
                .groupBy(tBucket)
                .orderBy(tBucket)
                .fetch(r -> new PremiumCandle(
                        r.value1().toInstant(ZoneOffset.UTC),
                        r.value2(),                                     // 시가
                        r.value3(),                                     // 고가
                        r.value4(),                                     // 저가
                        r.value5(),                                     // 종가
                        r.value6().setScale(4, RoundingMode.HALF_UP),   // 평균 (김프와 같은 소수 4자리)
                        r.value7()));                                   // 개수
    }

    private static Field<LocalDateTime> bucketOf(HistoryInterval interval) {
        return switch (interval) {
            case MINUTE -> PREMIUM_SNAPSHOT.CAPTURED_AT;          // 이미 분 단위로 저장돼 있음
            case HOUR -> truncate("HOUR", HOUR_ANCHOR);
            case DAY -> truncate("DAY", KST_DAY_ANCHOR);
        };
    }

    // 기준점부터 몇 단위가 지났는지 세고, 그만큼만 다시 더해서 "구간 시작 시각"으로 내림
    private static Field<LocalDateTime> truncate(String unit, LocalDateTime anchor) {
        return field("TIMESTAMPADD({0}, TIMESTAMPDIFF({0}, {1}, {2}), {1})",
                SQLDataType.LOCALDATETIME, keyword(unit), inline(anchor), PREMIUM_SNAPSHOT.CAPTURED_AT);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}