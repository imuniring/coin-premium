package com.uni.coinpremium.premium.adapter.in.web;

import com.uni.coinpremium.contract.registry.Coin;

import java.util.List;

/**
 * 지원 코인 목록 응답 (페이지네이션 필드를 붙일 수 있게 객체로 감쌈)
 */
public record CoinListResponse(List<CoinResponse> coins) {

    static CoinListResponse from(List<Coin> coins) {
        return new CoinListResponse(coins.stream().map(CoinResponse::from).toList());
    }
}
