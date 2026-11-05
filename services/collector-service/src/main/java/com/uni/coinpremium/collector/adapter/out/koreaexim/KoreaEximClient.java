package com.uni.coinpremium.collector.adapter.out.koreaexim;

import com.uni.coinpremium.collector.application.port.out.ExchangeRateFetchException;
import com.uni.coinpremium.collector.application.port.out.ExchangeRateFetcher;
import com.uni.coinpremium.contract.ExchangeRateEvent;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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

        } catch (IOException e) {
            // 타임아웃(HttpTimeoutException 포함), 네트워크 끊김 → 일시적
            throw new ExchangeRateFetchException("koreaexim request failed: " + e.getMessage(), e, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExchangeRateFetchException("koreaexim request interrupted", e, false);
        }
    }

    private URI buildUri(LocalDate date) {
        return URI.create(properties.url()
                + "?authkey=" + URLEncoder.encode(properties.apiKey(), StandardCharsets.UTF_8)
                + "&searchdate=" + date.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "&data=AP01");
    }
}