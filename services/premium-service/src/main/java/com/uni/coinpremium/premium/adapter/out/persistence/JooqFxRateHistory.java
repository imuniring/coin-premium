package com.uni.coinpremium.premium.adapter.out.persistence;

import static com.uni.coinpremium.premium.jooq.Tables.FX_RATE_HISTORY;

import com.uni.coinpremium.premium.application.port.out.FxRateHistory;
import com.uni.coinpremium.premium.domain.FxRate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
public class JooqFxRateHistory implements FxRateHistory {

    private final DSLContext dsl;

    public JooqFxRateHistory(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public void save(FxRate rate, String eventId) {
        dsl.insertInto(FX_RATE_HISTORY)
                .set(FX_RATE_HISTORY.RATE, rate.rate())
                .set(FX_RATE_HISTORY.PUBLISHED_AT, utc(rate.publishedAt()))
                .set(FX_RATE_HISTORY.EVENT_ID, eventId)
                .onDuplicateKeyIgnore()   // 기동 시 토픽 재소비로 같은 환율이 다시 와도 한 번만
                .execute();
    }

    @Override
    public Optional<FxRate> findLatestPublishedUpTo(Instant limit) {
        return dsl.select(FX_RATE_HISTORY.RATE, FX_RATE_HISTORY.PUBLISHED_AT)
                .from(FX_RATE_HISTORY)
                .where(FX_RATE_HISTORY.PUBLISHED_AT.le(utc(limit)))
                .orderBy(FX_RATE_HISTORY.PUBLISHED_AT.desc())
                .limit(1)
                .fetchOptional(r -> new FxRate(r.value1(), r.value2().toInstant(ZoneOffset.UTC)));
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}