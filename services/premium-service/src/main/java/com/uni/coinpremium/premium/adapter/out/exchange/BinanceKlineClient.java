package com.uni.coinpremium.premium.adapter.out.exchange;

import com.uni.coinpremium.premium.application.port.out.HistoricalPriceException;
import com.uni.coinpremium.premium.domain.MinuteClose;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Binance 1분 klines 조회
 * - GET /api/v3/klines?symbol=&interval=1m&startTime=&endTime=&limit=1000
 * - 응답: [openTime, open, high, low, close, ...] 배열의 배열, 시간 오름차순
 */
class BinanceKlineClient {

    private static final int MAX_LIMIT = 1000;
    private static final int CLOSE_INDEX = 4;   // [0]open time [1]open [2]high [3]low [4]close

    private final HttpClient http;
    private final JsonMapper json;
    private final URI baseUrl;
    private final Duration timeout;

    BinanceKlineClient(HttpClient http, JsonMapper json, URI baseUrl, Duration timeout) {
        this.http = http;
        this.json = json;
        this.baseUrl = baseUrl;
        this.timeout = timeout;
    }

    List<MinuteClose> minuteCloses(String symbol, Instant from, Instant to) {
        List<MinuteClose> result = new ArrayList<>();
        Instant cursor = from;
        while (cursor.isBefore(to)) {
            List<MinuteClose> page = parse(get(symbol, cursor, to));
            if (page.isEmpty()) {
                break;
            }
            result.addAll(page);
            if (page.size() < MAX_LIMIT) {
                break;   // 요청 구간을 다 받음
            }
            cursor = page.get(page.size() - 1).minuteStart().plusSeconds(60);   // 다음 분부터 이어서
        }
        return result;
    }

    private String get(String symbol, Instant from, Instant to) {
        // endTime은 그 시각을 포함하므로, [from, to)에 맞추려고 1ms 뺀다
        URI uri = URI.create(baseUrl + "/api/v3/klines?symbol=" + symbol + "&interval=1m"
                + "&startTime=" + from.toEpochMilli()
                + "&endTime=" + (to.toEpochMilli() - 1)
                + "&limit=" + MAX_LIMIT);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).GET().build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new HistoricalPriceException("binance klines failed: status=" + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new HistoricalPriceException("binance klines request failed: " + e.getClass().getSimpleName(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HistoricalPriceException("binance klines interrupted", e);
        }
    }

    /** 응답 JSON → MinuteClose (시간 오름차순) */
    List<MinuteClose> parse(String body) {
        List<MinuteClose> result = new ArrayList<>();
        for (JsonNode kline : json.readTree(body)) {
            Instant start = Instant.ofEpochMilli(kline.get(0).asLong());
            BigDecimal close = new BigDecimal(kline.get(CLOSE_INDEX).asText());   // 가격은 문자열로 옴 → 정밀도 보존
            result.add(new MinuteClose(start, close));
        }
        return result;
    }
}