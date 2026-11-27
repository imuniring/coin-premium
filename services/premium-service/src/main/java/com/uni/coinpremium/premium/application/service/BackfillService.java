package com.uni.coinpremium.premium.application.service;

import com.uni.coinpremium.contract.Exchange;
import com.uni.coinpremium.contract.registry.Coin;
import com.uni.coinpremium.premium.application.port.out.CoinCatalog;
import com.uni.coinpremium.premium.application.port.out.FxRateHistory;
import com.uni.coinpremium.premium.application.port.out.HistoricalPriceException;
import com.uni.coinpremium.premium.application.port.out.HistoricalPrices;
import com.uni.coinpremium.premium.application.port.out.PremiumSnapshotRepository;
import com.uni.coinpremium.premium.domain.FxRate;
import com.uni.coinpremium.premium.domain.FxSwitchRule;
import com.uni.coinpremium.premium.domain.GapRange;
import com.uni.coinpremium.premium.domain.KimchiPremium;
import com.uni.coinpremium.premium.domain.MinuteClose;
import com.uni.coinpremium.premium.domain.PriceQuote;
import com.uni.coinpremium.premium.domain.SnapshotSource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * 김프 스냅샷 공백 보정
 * - 분 m의 가격 = 시작 시각 m-1분 캔들의 종가 (실시간의 '직전 최신 체결'과 같은 기준)
 * - m-1분 캔들이 없으면 그 1분 체결이 없었던 것 → 채우지 않음 (실시간 신선도 규칙과 동일)
 * - 같은 분에 LIVE가 있으면 저장소가 무시 (실시간 값 우선)
 */
@Service
public class BackfillService {

    private static final Logger log = LoggerFactory.getLogger(BackfillService.class);
    private static final String DOMESTIC = "UPBIT";
    private static final String FOREIGN = "BINANCE";

    private final GapDetectionService gapDetection;
    private final CoinCatalog coinCatalog;
    private final HistoricalPrices historicalPrices;
    private final FxRateHistory fxRateHistory;
    private final FxSwitchRule fxSwitchRule;
    private final PremiumSnapshotRepository repository;

    public BackfillService(GapDetectionService gapDetection, CoinCatalog coinCatalog,
                           HistoricalPrices historicalPrices, FxRateHistory fxRateHistory,
                           FxSwitchRule fxSwitchRule, PremiumSnapshotRepository repository) {
        this.gapDetection = gapDetection;
        this.coinCatalog = coinCatalog;
        this.historicalPrices = historicalPrices;
        this.fxRateHistory = fxRateHistory;
        this.fxSwitchRule = fxSwitchRule;
        this.repository = repository;
    }

    /** 최근 구간의 공백을 찾아 채운다 */
    public BackfillResult backfillRecent() {
        BackfillResult total = BackfillResult.EMPTY;
        for (Map.Entry<String, List<GapRange>> entry : gapDetection.detect().entrySet()) {
            String symbol = entry.getKey();
            try (var sym = MDC.putCloseable("symbol", symbol)) {
                Coin coin = coinCatalog.find(symbol).orElseThrow();   // 공백 감지가 지원 코인만 돌려줌
                for (GapRange gap : entry.getValue()) {
                    total = total.plus(fill(coin, gap));
                }
            }
        }
        return total;
    }

    /** 공백 구간 하나 채우기 */
    BackfillResult fill(Coin coin, GapRange gap) {
        // 분 m에는 m-1분 캔들이 필요하므로 한 분 앞부터 조회: [from-1분, to) → 시작 시각 from-1분 ~ to-1분
        Instant queryFrom = gap.from().minus(1, ChronoUnit.MINUTES);
        Instant queryTo = gap.to();
        Map<Instant, MinuteClose> domestic;
        Map<Instant, MinuteClose> foreign;
        try {
            domestic = byStart(historicalPrices.minuteCloses(
                    Exchange.UPBIT, coin.marketOf(Exchange.UPBIT).orElseThrow(), queryFrom, queryTo));
            foreign = byStart(historicalPrices.minuteCloses(
                    Exchange.BINANCE, coin.marketOf(Exchange.BINANCE).orElseThrow(), queryFrom, queryTo));
        } catch (HistoricalPriceException e) {
            // 거래소 일시 장애 → 이 구간은 다음 실행 때 다시 시도
            log.atWarn()
                    .addKeyValue("from", gap.from())
                    .addKeyValue("to", gap.to())
                    .log("[backfill] 거래소 조회 실패, 다음 실행 때 재시도: {}", e.getMessage());
            return new BackfillResult(0, 0, (int) gap.minutes());
        }

        int filled = 0;
        int unfillable = 0;
        for (Instant m = gap.from(); !m.isAfter(gap.to()); m = m.plus(1, ChronoUnit.MINUTES)) {
            Instant candleStart = m.minus(1, ChronoUnit.MINUTES);   // m을 채우는 캔들 = 1분 앞
            MinuteClose d = domestic.get(candleStart);
            MinuteClose f = foreign.get(candleStart);
            Optional<FxRate> fx = fxRateHistory.findLatestPublishedUpTo(fxSwitchRule.publishedAtLimit(m));
            if (d == null || f == null || fx.isEmpty()) {
                unfillable++;   // 그 1분 체결이 없었거나 환율 이력이 없음
                continue;
            }
            if (repository.save(snapshot(coin.symbol(), m, d, f, fx.get()), m, SnapshotSource.BACKFILL)) {
                filled++;
            }
        }

        log.atInfo()
                .addKeyValue("from", gap.from())
                .addKeyValue("to", gap.to())
                .addKeyValue("filled", filled)
                .addKeyValue("unfillable", unfillable)
                .log("[backfill] 공백 보정 {} ~ {} - 채움 {}, 못 채움 {}", gap.from(), gap.to(), filled, unfillable);
        return new BackfillResult(filled, unfillable, 0);
    }

    // 실시간과 같은 공식(KimchiPremium.calculate)으로 계산, 체결 시각은 캔들 시작 시각
    private static PremiumSnapshot snapshot(String symbol, Instant m, MinuteClose d, MinuteClose f, FxRate fx) {
        KimchiPremium premium = KimchiPremium.calculate(symbol, d.close(), f.close(), fx.rate(), m);
        return new PremiumSnapshot(premium,
                new PriceQuote(DOMESTIC, symbol, d.close(), d.minuteStart()),
                new PriceQuote(FOREIGN, symbol, f.close(), f.minuteStart()),
                fx,
                false);   // 정확히 1분 앞 캔들만 쓰므로 신선
    }

    private static Map<Instant, MinuteClose> byStart(List<MinuteClose> closes) {
        return closes.stream().collect(Collectors.toMap(MinuteClose::minuteStart, Function.identity(), (a, b) -> a));
    }
}