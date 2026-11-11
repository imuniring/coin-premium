package com.uni.coinpremium.premium.adapter.in.web;

import com.uni.coinpremium.premium.application.exception.MarketDataNotReadyException;
import com.uni.coinpremium.premium.application.exception.UnsupportedSymbolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 애플리케이션 예외 → HTTP 응답(ProblemDetail) 변환
 */
@RestControllerAdvice
public class PremiumExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PremiumExceptionHandler.class);
    private static final String RETRY_AFTER_SECONDS = "5";

    // 수집하지 않는 코인 → 404 (재시도해도 소용없음)
    @ExceptionHandler(UnsupportedSymbolException.class)
    public ProblemDetail handleUnsupported(UnsupportedSymbolException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // 데이터 준비 전 → 503 + Retry-After (잠시 후 재시도하면 됨)
    @ExceptionHandler(MarketDataNotReadyException.class)
    public ResponseEntity<ProblemDetail> handleNotReady(MarketDataNotReadyException e) {
        log.info("[premium] 데이터 준비 전 조회 - {}", e.getMessage());   // 기동 직후엔 정상 상황이라 info

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        problem.setProperty("missing", e.missing());   // ["BINANCE"] 처럼 무엇이 없는지

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .body(problem);
    }
}