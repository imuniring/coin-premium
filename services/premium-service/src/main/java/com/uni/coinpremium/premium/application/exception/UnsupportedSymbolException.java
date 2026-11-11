package com.uni.coinpremium.premium.application.exception;

/**
 * 수집하지 않는 코인 (기다려도 생기지 않음)
 */
public class UnsupportedSymbolException extends RuntimeException {

    public UnsupportedSymbolException(String symbol) {
        super("지원하지 않는 코인입니다: " + symbol);
    }
}