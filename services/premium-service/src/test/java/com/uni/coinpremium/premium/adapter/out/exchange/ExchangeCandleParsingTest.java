package com.uni.coinpremium.premium.adapter.out.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.uni.coinpremium.premium.domain.MinuteClose;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ExchangeCandleParsingTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newHttpClient();
    private final URI unused = URI.create("http://localhost");

    @Test
    @DisplayName("Upbit: candle_date_time_utc(시간대 표기 없음)를 UTC 분 시작 시각으로, trade_price를 종가로 읽는다")
    void upbit() {
        String body = """
                [
                  {"market":"KRW-BTC","candle_date_time_utc":"2026-11-27T10:02:00","trade_price":107300000.0},
                  {"market":"KRW-BTC","candle_date_time_utc":"2026-11-27T10:01:00","trade_price":107250000.0}
                ]""";

        List<MinuteClose> closes = new UpbitCandleClient(http, json, unused, Duration.ofSeconds(1)).parse(body);

        assertEquals(new MinuteClose(Instant.parse("2026-11-27T10:01:00Z"), new BigDecimal("107250000.0")), closes.get(1));
    }

    @Test
    @DisplayName("Binance: 배열 첫 값을 분 시작 시각, 다섯 번째 값을 종가로 읽는다 (문자열 가격 정밀도 보존)")
    void binance() {
        // [open time, open, high, low, close, volume, close time, ...]
        String body = """
                [[1795600860000,"79500.00","79560.00","79480.00","79528.57000000","12.5",1795600919999]]""";

        List<MinuteClose> closes = new BinanceKlineClient(http, json, unused, Duration.ofSeconds(1)).parse(body);

        assertEquals(List.of(new MinuteClose(Instant.ofEpochMilli(1795600860000L), new BigDecimal("79528.57000000"))), closes);
    }
}