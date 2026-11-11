package com.uni.coinpremium.premium.adapter.in.web;

import com.uni.coinpremium.premium.application.service.PremiumQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@RestController
@RequestMapping("/premium")
public class PremiumController {

    private final PremiumQueryService queryService;

    public PremiumController(PremiumQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public PremiumResponse getPremium(@RequestParam String symbol) {
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);   // " btc " → "BTC"
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbol은 비어 있을 수 없습니다");
        }
        return PremiumResponse.from(queryService.getPremium(normalized));
    }
}