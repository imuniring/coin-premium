package com.uni.coinpremium.premium.adapter.in.web;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 경로의 코인 심볼 정리 (컨트롤러 공통)
 */
final class SymbolPath {

    // 영문 대문자·숫자 1~20자 (DB 컬럼 VARCHAR(20)과 맞춤)
    private static final Pattern FORMAT = Pattern.compile("^[A-Z0-9]{1,20}$");

    private SymbolPath() {}

    static String normalize(String symbol) {
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(normalized).matches()) {
            throw new InvalidSymbolException();
        }
        return normalized;
    }
}
