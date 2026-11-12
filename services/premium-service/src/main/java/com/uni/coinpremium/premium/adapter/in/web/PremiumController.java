package com.uni.coinpremium.premium.adapter.in.web;

import com.uni.coinpremium.premium.application.service.PremiumHistoryService;
import com.uni.coinpremium.premium.application.service.PremiumQueryService;
import com.uni.coinpremium.premium.domain.HistoryInterval;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 코인별 김프 조회 API
 */
@Tag(name = "Premium", description = "김치프리미엄 조회")
@RestController
@RequestMapping("/v1/coins/{symbol}/premium")
public class PremiumController {

    // 영문 대문자·숫자 1~20자 (DB 컬럼 VARCHAR(20)과 맞춤)
    private static final Pattern SYMBOL_FORMAT = Pattern.compile("^[A-Z0-9]{1,20}$");

    private final PremiumQueryService queryService;
    private final PremiumHistoryService historyService;

    public PremiumController(PremiumQueryService queryService, PremiumHistoryService historyService) {
        this.queryService = queryService;
        this.historyService = historyService;
    }

    @Operation(summary = "현재 김프 조회",
            description = "국내(Upbit) 최신 체결가, 해외(Binance) 최신 체결가, 최신 환율로 계산한 현재 김프. "
                    + "기동 직후 데이터가 준비되지 않았으면 503과 Retry-After를 반환한다.")
    @GetMapping
    public PremiumResponse getPremium(
            @Parameter(description = "코인 심볼 (대소문자 무관)", example = "BTC") @PathVariable String symbol) {
        return PremiumResponse.from(queryService.getPremium(normalize(symbol)));
    }

    @Operation(summary = "김프 추이 조회",
            description = "1분 스냅샷을 구간별 캔들(시가·고가·저가·종가·평균·개수)로 집계한다. "
                    + "범위는 [from, to), 1d는 KST 자정 기준. 최대 기간: 1m 1일, 1h 31일, 1d 366일.")
    @GetMapping("/history")
    public PremiumHistoryResponse getHistory(
            @Parameter(description = "코인 심볼", example = "BTC") @PathVariable String symbol,
            @Parameter(description = "시작 시각 (포함, UTC). 생략 시 to - 1일", example = "2026-11-11T00:00:00Z")
            @RequestParam(required = false) Instant from,
            @Parameter(description = "종료 시각 (미포함, UTC). 생략 시 현재", example = "2026-11-12T00:00:00Z")
            @RequestParam(required = false) Instant to,
            @Parameter(description = "구간 간격: 1m, 1h, 1d", example = "1h")
            @RequestParam(defaultValue = "1h") String interval) {
        HistoryInterval parsed = HistoryInterval.fromCode(interval).orElseThrow(InvalidIntervalException::new);
        return PremiumHistoryResponse.from(historyService.getHistory(normalize(symbol), parsed, from, to));
    }

    // " btc " → "BTC", 형식이 틀리면 400 (입력값은 응답에 넣지 않음)
    private static String normalize(String symbol) {
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        if (!SYMBOL_FORMAT.matcher(normalized).matches()) {
            throw new InvalidSymbolException();
        }
        return normalized;
    }
}