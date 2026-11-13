package com.uni.coinpremium.premium.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    @DisplayName("헤더가 없으면 새 ID를 만들어 MDC와 응답 헤더에 넣고, 요청이 끝나면 MDC에서 지운다")
    void generatesIdWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();

        // 체인 안(= 컨트롤러 실행 중)에서 MDC 값을 잡아 둔다
        filter.doFilter(request, response, (req, res) -> seen.set(MDC.get(RequestIdFilter.MDC_KEY)));

        assertThat(seen.get()).isNotBlank();
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(seen.get());
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("유효한 X-Request-Id 헤더가 있으면 그대로 사용한다")
    void reusesValidHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> seen.set(MDC.get(RequestIdFilter.MDC_KEY)));

        assertThat(seen.get()).isEqualTo("abc-123");
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("abc-123");
    }

    @Test
    @DisplayName("형식이 틀린 헤더(줄바꿈 포함)는 무시하고 새 ID를 만든다")
    void ignoresInvalidHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "abc\n[ERROR] 가짜 로그");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(RequestIdFilter.HEADER))
                .isNotEqualTo("abc\n[ERROR] 가짜 로그")
                .matches("[A-Za-z0-9-]{1,64}");
    }
}