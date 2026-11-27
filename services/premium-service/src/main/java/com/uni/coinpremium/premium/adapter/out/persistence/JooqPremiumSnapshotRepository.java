package com.uni.coinpremium.premium.adapter.out.persistence;

import com.uni.coinpremium.premium.application.port.out.PremiumSnapshotRepository;
import com.uni.coinpremium.premium.application.port.out.SnapshotTimeline;
import com.uni.coinpremium.premium.application.service.PremiumSnapshot;
import com.uni.coinpremium.premium.domain.SnapshotSource;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.uni.coinpremium.premium.jooq.Tables.PREMIUM_SNAPSHOT;

/**
 * jOOQ 기반 스냅샷 저장
 */
@Repository
public class JooqPremiumSnapshotRepository implements PremiumSnapshotRepository, SnapshotTimeline {

    private final DSLContext dsl;

    public JooqPremiumSnapshotRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public boolean save(PremiumSnapshot s, Instant capturedAt, SnapshotSource source) {
        int inserted = dsl.insertInto(PREMIUM_SNAPSHOT)
                .set(PREMIUM_SNAPSHOT.SYMBOL, s.premium().symbol())
                .set(PREMIUM_SNAPSHOT.PREMIUM_RATE, s.premium().rate())
                .set(PREMIUM_SNAPSHOT.DOMESTIC_PRICE, s.domestic().price())
                .set(PREMIUM_SNAPSHOT.FOREIGN_PRICE, s.foreign().price())
                .set(PREMIUM_SNAPSHOT.EXCHANGE_RATE, s.fxRate().rate())
                .set(PREMIUM_SNAPSHOT.DOMESTIC_TRADED_AT, utc(s.domestic().tradedAt()))
                .set(PREMIUM_SNAPSHOT.FOREIGN_TRADED_AT, utc(s.foreign().tradedAt()))
                .set(PREMIUM_SNAPSHOT.FX_PUBLISHED_AT, utc(s.fxRate().publishedAt()))
                .set(PREMIUM_SNAPSHOT.CAPTURED_AT, utc(capturedAt))
                .set(PREMIUM_SNAPSHOT.SOURCE, source.name())
                .onDuplicateKeyIgnore()   // 같은 (symbol, captured_at)이 있으면 조용히 건너뜀
                .execute();
        return inserted == 1;
    }

    @Override
    public List<Instant> capturedMinutes(String symbol, Instant from, Instant to) {
        return dsl.select(PREMIUM_SNAPSHOT.CAPTURED_AT)
                .from(PREMIUM_SNAPSHOT)
                .where(PREMIUM_SNAPSHOT.SYMBOL.eq(symbol)
                        .and(PREMIUM_SNAPSHOT.CAPTURED_AT.ge(utc(from)))
                        .and(PREMIUM_SNAPSHOT.CAPTURED_AT.lt(utc(to))))
                .fetch(r -> r.value1().toInstant(ZoneOffset.UTC));   // DB의 UTC 시각 → Instant
    }

    // Instant(UTC 시각) → UTC 기준 LocalDateTime (DB의 DATETIME은 시간대 정보가 없음)
    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}