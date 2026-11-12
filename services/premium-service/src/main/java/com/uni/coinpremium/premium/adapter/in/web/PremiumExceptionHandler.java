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

    @ExceptionHandler(InvalidSymbolException.class)
    public ProblemDetail handleInvalid(InvalidSymbolException e) {
        return problem(ErrorCode.INVALID_SYMBOL, e.getMessage());
    }

    @ExceptionHandler(UnsupportedSymbolException.class)
    public ProblemDetail handleUnsupported(UnsupportedSymbolException e) {
        return problem(ErrorCode.UNSUPPORTED_SYMBOL, e.getMessage());
    }

    @ExceptionHandler(MarketDataNotReadyException.class)
    public ResponseEntity<ProblemDetail> handleNotReady(MarketDataNotReadyException e) {
        log.info("[premium] 데이터 준비 전 조회 - {}", e.getMessage());
        ProblemDetail problem = problem(ErrorCode.MARKET_DATA_NOT_READY, e.getMessage());
        problem.setProperty("missing", e.missing());
        return ResponseEntity.status(ErrorCode.MARKET_DATA_NOT_READY.status())
                .header(HttpHeaders.RETRY_AFTER, "5")
                .body(problem);
    }

    // 모든 에러 응답을 같은 모양으로
    private static ProblemDetail problem(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setProperty("code", code.name());   // 클라이언트는 이걸로 분기
        return problem;
    }
}