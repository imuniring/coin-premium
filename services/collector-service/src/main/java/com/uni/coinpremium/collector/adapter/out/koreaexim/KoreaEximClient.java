package com.uni.coinpremium.collector.adapter.out.koreaexim;

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
            if (response.statusCode() != 200) {
                throw new IllegalStateException("koreaexim http status=" + response.statusCode());
            }
            KoreaEximRate[] rates = jsonMapper.readValue(response.body(), KoreaEximRate[].class);
            return KoreaEximRateMapper.toUsdKrw(rates, date, Instant.now());

        } catch (IOException e) {
            throw new IllegalStateException("koreaexim request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // 인터럽트 상태 복구 (스레드 종료 신호를 삼키지 않음)
            throw new IllegalStateException("koreaexim request interrupted", e);
        }
    }

    private URI buildUri(LocalDate date) {
        return URI.create(properties.url()
                + "?authkey=" + URLEncoder.encode(properties.apiKey(), StandardCharsets.UTF_8)
                + "&searchdate=" + date.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "&data=AP01");
    }
}