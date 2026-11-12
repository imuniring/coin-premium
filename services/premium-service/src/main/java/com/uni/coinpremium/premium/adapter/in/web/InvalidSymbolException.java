package com.uni.coinpremium.premium.adapter.in.web;

/**
 * 경로의 심볼 형식이 잘못됨 (HTTP 입력 검증이라 web 어댑터에 둠)
 */
class InvalidSymbolException extends RuntimeException {
    InvalidSymbolException() {
        super("symbol은 영문 대문자·숫자 1~20자여야 합니다.");
    }
}
