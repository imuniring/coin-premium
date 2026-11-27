package com.uni.coinpremium.premium.adapter.in.web;

import com.uni.coinpremium.premium.application.service.CoinQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지원 코인 조회 API
 */
@Tag(name = "Coin", description = "지원 코인 조회")
@RestController
@RequestMapping("/v1/coins")
public class CoinController {

    private final CoinQueryService coinQueryService;

    public CoinController(CoinQueryService coinQueryService) {
        this.coinQueryService = coinQueryService;
    }

    @Operation(summary = "지원 코인 목록", description = "김프를 조회할 수 있는 코인과 거래소별 마켓 코드")
    @GetMapping
    public CoinListResponse getCoins() {
        return CoinListResponse.from(coinQueryService.getCoins());
    }

    @Operation(summary = "코인 상세", description = "지원하지 않는 코인이면 404")
    @GetMapping("/{symbol}")
    public CoinResponse getCoin(
            @Parameter(description = "코인 심볼 (대소문자 무관)", example = "BTC") @PathVariable String symbol) {
        return CoinResponse.from(coinQueryService.getCoin(SymbolPath.normalize(symbol)));
    }
}