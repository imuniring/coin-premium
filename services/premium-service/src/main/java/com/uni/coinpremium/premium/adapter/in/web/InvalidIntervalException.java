package com.uni.coinpremium.premium.adapter.in.web;

class InvalidIntervalException extends RuntimeException {
    InvalidIntervalException() {
        super("interval은 1m, 1h, 1d 중 하나여야 합니다.");
    }
}