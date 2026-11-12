package com.uni.coinpremium.premium.adapter.in.web;

import com.uni.coinpremium.premium.application.service.PremiumHistoryService;
import com.uni.coinpremium.premium.application.service.PremiumQueryService;
import com.uni.coinpremium.premium.domain.HistoryInterval;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 코인별 김프 조회 API
 */
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

    // GET /v1/coins/BTC/premium
    @GetMapping
    public PremiumResponse getPremium(@PathVariable String symbol) {
        return PremiumResponse.from(queryService.getPremium(normalize(symbol)));
    }

    // GET /v1/coins/BTC/premium/history?from=2026-11-11T00:00:00Z&to=2026-11-12T00:00:00Z&interval=1h
    @GetMapping("/history")
    public PremiumHistoryResponse getHistory(@PathVariable String symbol,
                                             @RequestParam(required = false) Instant from,
                                             @RequestParam(required = false) Instant to,
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