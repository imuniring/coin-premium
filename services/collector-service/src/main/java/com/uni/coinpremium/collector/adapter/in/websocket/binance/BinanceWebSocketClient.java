package com.uni.coinpremium.collector.adapter.in.websocket.binance;

import com.uni.coinpremium.collector.adapter.in.websocket.ExchangeWebSocketClient;
import com.uni.coinpremium.collector.application.port.out.TickerPublisher;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Instant;

/**
 * Binance 체결 수신 (URL로 구독하므로 구독 메시지 없음)
 */
@Component
public class BinanceWebSocketClient extends ExchangeWebSocketClient {

    private final BinanceProperties properties;
    private final JsonMapper jsonMapper;
    private final TickerPublisher publisher;

    public BinanceWebSocketClient(BinanceProperties properties, JsonMapper jsonMapper, TickerPublisher publisher) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.publisher = publisher;
    }

    @Override
    protected String name() {
        return "binance";
    }

    @Override
    protected URI uri() {
        return properties.streamUri();
    }

    @Override
    protected void onMessage(String message, Instant receivedAt) {
        BinanceStreamMessage msg = jsonMapper.readValue(message, BinanceStreamMessage.class);
        if (msg.data() == null || !msg.data().isTrade()) {
            return;
        }
        publisher.publish(BinanceTradeMapper.toEvent(msg.data(), receivedAt));
    }
}