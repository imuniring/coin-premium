package com.uni.coinpremium.collector.adapter.out.koreaexim;

import com.uni.coinpremium.collector.application.port.out.ExchangeRateFetchException;
import com.uni.coinpremium.collector.application.port.out.ExchangeRateFetcher;
import com.uni.coinpremium.contract.ExchangeRateEvent;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 수출입은행 환율 API 클라이언트
 * - 주의: 요청 URL에 인증키가 포함되므로 URL을 로그에 남기지 않는다
 */
@Component
public class KoreaEximClient implements ExchangeRateFetcher {

    private final KoreaEximProperties properties;
    private final JsonMapper jsonMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public KoreaEximClient(KoreaEximProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Retry(name = "koreaexim")
    @CircuitBreaker(name = "koreaexim", fallbackMethod = "circuitOpen")
    public Optional<ExchangeRateEvent> fetchUsdKrw(LocalDate date) {
        HttpRequest request = HttpRequest.newBuilder(buildUri(date))
                .timeout(Duration.ofSeconds(5))   // 응답이 안 오면 무한 대기하지 않게
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 500) {
                throw new ExchangeRateFetchException("koreaexim server error status=" + status, true);    // 서버 문제 → 재시도
            }
            if (status != 200) {
                throw new ExchangeRateFetchException("koreaexim http status=" + status, false);   // 요청 문제 → 재시도 무의미
            }
            KoreaEximRate[] rates = jsonMapper.readValue(response.body(), KoreaEximRate[].class);
            return KoreaEximRateMapper.toUsdKrw(rates, date, Instant.now());

        } catch (JacksonException e) {
            // 응답 형식이 예상과 다름 (점검 페이지 등) → 다시 요청해도 같은 응답일 가능성이 높음
            throw new ExchangeRateFetchException("koreaexim response parse failed", e, false);
        } catch (IOException e) {
            // 타임아웃·네트워크 끊김(핸드셰이크 중 끊김 포함)은 일시적, 인증서 검증 실패는 다시 해도 같은 결과
            boolean retryable = !isCertificateFailure(e);
            throw new ExchangeRateFetchException("koreaexim request failed: " + describe(e), e, retryable);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExchangeRateFetchException("koreaexim request interrupted", e, false);
        }
    }

    /**
     * 서킷이 열려 호출이 차단됐을 때만 실행 (파라미터 타입이 CallNotPermittedException이라 다른 예외는 그대로 전파)
     * application 계층이 Resilience4j 예외를 몰라도 되도록 포트의 예외로 바꿔 던진다
     */
    @SuppressWarnings("unused")   // Resilience4j가 fallbackMethod 이름으로 찾아 호출
    private Optional<ExchangeRateEvent> circuitOpen(LocalDate date, CallNotPermittedException e) {
        throw new ExchangeRateFetchException("koreaexim circuit open - 호출 차단 중", e, false);
    }

    private URI buildUri(LocalDate date) {
        return URI.create(properties.url()
                + "?authkey=" + URLEncoder.encode(properties.apiKey(), StandardCharsets.UTF_8)
                + "&searchdate=" + date.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "&data=AP01");
    }

    /**
     * 원인 체인에 인증서 검증 실패가 있는지 확인
     * - PKIX 오류는 ValidatorException, CertPathBuilderException 등 GeneralSecurityException 계열을 원인으로 가진다
     * - 핸드셰이크 도중 연결이 끊긴 경우(SocketException)는 SSLHandshakeException이어도 일시적 실패로 본다
     */
    static boolean isCertificateFailure(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof GeneralSecurityException) {
                return true;
            }
        }
        return false;
    }

    // 메시지가 없는 예외(ConnectException 등)도 원인을 알 수 있게 예외 이름을 함께 남긴다
    private static String describe(Throwable e) {
        return e.getMessage() == null
                ? e.getClass().getSimpleName()
                : e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}