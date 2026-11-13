package com.uni.coinpremium.premium.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 requestId를 MDC에 넣어 같은 요청의 로그를 묶는다.
 * - 요청 헤더에 유효한 X-Request-Id가 있으면 재사용 (추후 gateway가 넘겨주는 값)
 * - 없거나 형식이 틀리면 새로 생성
 * - 응답 헤더에도 같은 값을 넣어, 에러 신고 시 ID로 로그를 찾을 수 있게 한다
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)   // 다른 필터의 로그에도 ID가 붙도록 가장 먼저 실행
public class RequestIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    static final String HEADER = "X-Request-Id";
    static final String MDC_KEY = "requestId";

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = resolve(request.getHeader(HEADER));
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            logAccess(request, response, start);   // MDC를 지우기 전에 남겨야 requestId가 붙음
            MDC.remove(MDC_KEY);
        }
    }

    // 요청 완료 로그: 메시지는 사람이 읽는 용도, key-value는 검색·집계 용도
    private static void logAccess(HttpServletRequest request, HttpServletResponse response, long start) {
        long durationMs = (System.nanoTime() - start) / 1_000_000;
        log.atInfo()
                .addKeyValue("httpMethod", request.getMethod())
                .addKeyValue("path", request.getRequestURI())   // 쿼리 스트링은 제외 (입력값 노출 방지)
                .addKeyValue("status", response.getStatus())
                .addKeyValue("durationMs", durationMs)
                .log("[access] {} {} {} {}ms",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
    }

    private static String resolve(String header) {
        return header != null && VALID.matcher(header).matches()
                ? header
                : UUID.randomUUID().toString();
    }
}