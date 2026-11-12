package com.uni.coinpremium.premium.adapter.in.web;

import org.springframework.http.HttpStatus;

import java.net.URI;

/**
 * API 에러 코드 (클라이언트는 code로 분기, 문구는 바뀔 수 있음)
 */
public enum ErrorCode {

    INVALID_SYMBOL(HttpStatus.BAD_REQUEST),
    UNSUPPORTED_SYMBOL(HttpStatus.NOT_FOUND),
    MARKET_DATA_NOT_READY(HttpStatus.SERVICE_UNAVAILABLE);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}