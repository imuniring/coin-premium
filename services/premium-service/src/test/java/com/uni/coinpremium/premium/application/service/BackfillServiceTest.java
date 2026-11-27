package com.uni.coinpremium.premium.application.service;

import static com.uni.coinpremium.contract.Exchange.BINANCE;
import static com.uni.coinpremium.contract.Exchange.UPBIT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.uni.coinpremium.contract.registry.Coin;
import com.uni.coinpremium.premium.application.port.out.FxRateHistory;
import com.uni.coinpremium.premium.application.port.out.HistoricalPriceException;
import com.uni.coinpremium.premium.application.port.out.HistoricalPrices;
import com.uni.coinpremium.premium.domain.FxRate;
import com.uni.coinpremium.premium.domain.FxSwitchRule;
import com.uni.coinpremium.premium.domain.GapRange;
import com.uni.coinpremium.premium.domain.MinuteClose;
import com.uni.coinpremium.premium.domain.SnapshotSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BackfillServiceTest {

    private static final Coin BTC = new Coin("BTC", "비트코인", Map.of(UPBIT, "KRW-BTC", BINANCE, "BTCUSDT"));
    // 11/27 14:00 KST 무렵 (환율 전환 이후)
    private static final Instant T0 = Instant.parse("2026-11-27T05:00:00Z");

    private record Saved(Instant capturedAt, SnapshotSource source, BigDecimal domesticPrice) {}
    private final List<Saved> saved = new ArrayList<>();

    private final FxRateHistory fxHistory = new FxRateHistory() {
        @Override
        public void save(FxRate rate, String eventId) {
        }

        @Override
        public Optional<FxRate> findLatestPublishedUpTo(Instant limit) {
            return Optional.of(new FxRate(new BigDecimal("1340.0"), limit));
        }
    };
    private final FxSwitchRule rule = new FxSwitchRule(LocalTime.of(11, 0), ZoneId.of("Asia/Seoul"));

    private BackfillService serviceWith(HistoricalPrices prices) {
        return new BackfillService(null, null, prices, fxHistory, rule,
                (s, at, src) -> saved.add(new Saved(at, src, s.domestic().price())));
    }

    @Test
    @DisplayName("분 m은 시작 시각이 m-1분인 캔들의 종가로 채우고 BACKFILL로 저장한다")
    void fillWithPreviousMinuteCandle() {
        // 공백: T0+2분 ~ T0+3분 → 필요한 캔들: T0+1분, T0+2분 시작
        HistoricalPrices prices = (exchange, market, from, to) -> List.of(
                close(1, exchange == UPBIT ? "100" : "0.07"),
                close(2, exchange == UPBIT ? "200" : "0.07"),
                close(3, exchange == UPBIT ? "300" : "0.07"));   // T0+3분 캔들은 미래 가격 → 쓰이면 안 됨

        BackfillResult result = serviceWith(prices).fill(BTC, gap(2, 3));

        assertEquals(new BackfillResult(2, 0, 0), result);
        assertEquals(List.of(
                        new Saved(min(2), SnapshotSource.BACKFILL, new BigDecimal("100")),    // T0+2분 ← T0+1분 캔들
                        new Saved(min(3), SnapshotSource.BACKFILL, new BigDecimal("200"))),   // T0+3분 ← T0+2분 캔들
                saved);
    }

    @Test
    @DisplayName("1분 앞 캔들이 한쪽 거래소에 없으면 그 분은 채우지 않는다")
    void skipWhenPreviousCandleMissing() {
        HistoricalPrices prices = (exchange, market, from, to) -> exchange == UPBIT
                ? List.of(close(1, "100"), close(2, "200"))
                : List.of(close(2, "0.07"));   // Binance는 T0+1분 캔들 없음 (그 1분 체결 없음)

        BackfillResult result = serviceWith(prices).fill(BTC, gap(2, 3));

        assertEquals(new BackfillResult(1, 1, 0), result);   // T0+2분 못 채움, T0+3분 채움
        assertEquals(min(3), saved.get(0).capturedAt());
    }

    @Test
    @DisplayName("거래소 조회가 실패하면 구간 전체를 실패로 세고 다음 실행에 맡긴다")
    void exchangeFailure() {
        HistoricalPrices prices = (exchange, market, from, to) -> {
            throw new HistoricalPriceException("upbit candles failed: status=503");
        };

        BackfillResult result = serviceWith(prices).fill(BTC, gap(2, 4));

        assertEquals(new BackfillResult(0, 0, 3), result);
        assertEquals(List.of(), saved);
    }

    private static MinuteClose close(int minute, String price) {
        return new MinuteClose(min(minute), new BigDecimal(price));
    }

    private static Instant min(int minute) {
        return T0.plusSeconds(60L * minute);
    }

    private static GapRange gap(int fromMin, int toMin) {
        return new GapRange(min(fromMin), min(toMin));
    }
}