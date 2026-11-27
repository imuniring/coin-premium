package com.uni.coinpremium.premium.adapter.out.exchange;

import com.uni.coinpremium.premium.application.port.out.HistoricalPriceException;
import com.uni.coinpremium.premium.domain.MinuteClose;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Upbit 1분 캔들 조회
 * - GET /v1/candles/minutes/1?market=&to=&count=200
 * - to는 그 시각 미포함, 응답은 최신→과거 순, 호출당 최대 200개
 */
class UpbitCandleClient {

    private static final int MAX_COUNT = 200;

    private final HttpClient http;
    private final JsonMapper json;
    private final URI baseUrl;
    private final Duration timeout;

    UpbitCandleClient(HttpClient http, JsonMapper json, URI baseUrl, Duration timeout) {
        this.http = http;
        // 가격 숫자를 double이 아니라 BigDecimal로 읽어 소수점 오차 방지 (소수점 가격 코인 대비)
        this.json = json.rebuild().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build();
        this.baseUrl = baseUrl;
        this.timeout = timeout;
    }

    List<MinuteClose> minuteCloses(String market, Instant from, Instant to) {
        List<MinuteClose> result = new ArrayList<>();
        Instant cursor = to;   // 이 시각 이전 캔들을 요청 → 응답의 가장 오래된 캔들 시각으로 계속 과거로
        while (cursor.isAfter(from)) {
            List<MinuteClose> page = parse(get(market, cursor));
            if (page.isEmpty()) {
                break;   // 더 과거에 캔들이 없음
            }
            page.stream().filter(c -> !c.minuteStart().isBefore(from)).forEach(result::add);
            Instant oldest = page.get(page.size() - 1).minuteStart();
            if (page.size() < MAX_COUNT || !oldest.isAfter(from)) {
                break;   // 요청 구간을 다 받음
            }
            cursor = oldest;
        }
        result.sort(Comparator.comparing(MinuteClose::minuteStart));   // 포트 약속: 시간 오름차순
        return result;
    }

    private String get(String market, Instant to) {
        URI uri = URI.create(baseUrl + "/v1/candles/minutes/1"
                + "?market=" + URLEncoder.encode(market, StandardCharsets.UTF_8)
                + "&to=" + URLEncoder.encode(to.toString(), StandardCharsets.UTF_8)
                + "&count=" + MAX_COUNT);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).GET().build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new HistoricalPriceException("upbit candles failed: status=" + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new HistoricalPriceException("upbit candles request failed: " + e.getClass().getSimpleName(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HistoricalPriceException("upbit candles interrupted", e);
        }
    }

    /** 응답 JSON → MinuteClose (응답 순서 그대로: 최신→과거) */
    List<MinuteClose> parse(String body) {
        List<MinuteClose> result = new ArrayList<>();
        for (JsonNode candle : json.readTree(body)) {
            // "2026-11-27T10:01:00" (시간대 표기 없음, UTC) → 캔들 시작 시각
            Instant start = LocalDateTime.parse(candle.get("candle_date_time_utc").asText()).toInstant(ZoneOffset.UTC);
            result.add(new MinuteClose(start, candle.get("trade_price").decimalValue()));
        }
        return result;
    }
}