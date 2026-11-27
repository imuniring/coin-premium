package com.uni.coinpremium.premium.adapter.out.exchange;

import com.uni.coinpremium.contract.Exchange;
import com.uni.coinpremium.premium.application.port.out.HistoricalPrices;
import com.uni.coinpremium.premium.config.ExchangeApiProperties;
import com.uni.coinpremium.premium.domain.MinuteClose;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 거래소별 과거 1분 종가 조회를 한 포트로 묶는다
 */
@Component
public class ExchangeHistoricalPrices implements HistoricalPrices {

    private final UpbitCandleClient upbit;
    private final BinanceKlineClient binance;

    public ExchangeHistoricalPrices(JsonMapper json, ExchangeApiProperties properties) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        this.upbit = new UpbitCandleClient(http, json, properties.upbitUrl(), properties.timeout());
        this.binance = new BinanceKlineClient(http, json, properties.binanceUrl(), properties.timeout());
    }

    @Override
    public List<MinuteClose> minuteCloses(Exchange exchange, String market, Instant from, Instant to) {
        return switch (exchange) {
            case UPBIT -> upbit.minuteCloses(market, from, to);
            case BINANCE -> binance.minuteCloses(market, from, to);
        };
    }
}