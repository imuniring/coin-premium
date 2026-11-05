package com.uni.coinpremium.collector.adapter.in.websocket.upbit;

import com.uni.coinpremium.collector.adapter.in.websocket.ExchangeWebSocketClient;
import com.uni.coinpremium.collector.application.port.out.TickerPublisher;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Upbit 체결 수신
 */
@Component
public class UpbitWebSocketClient extends ExchangeWebSocketClient {

    private final UpbitProperties properties;
    private final JsonMapper jsonMapper;
    private final TickerPublisher publisher;

    public UpbitWebSocketClient(UpbitProperties properties, JsonMapper jsonMapper, TickerPublisher publisher) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.publisher = publisher;
    }

    @Override
    protected String name() {
        return "upbit";
    }

    @Override
    protected URI uri() {
        return properties.url();
    }

    // 예: [{"ticket":"..."},{"type":"trade","codes":["KRW-BTC","KRW-ETH"]}]
    @Override
    protected String subscribeMessage() {
        String codes = properties.codes().stream()
                .map(code -> "\"" + code + "\"")
                .collect(Collectors.joining(","));
        return "[{\"ticket\":\"%s\"},{\"type\":\"trade\",\"codes\":[%s]}]"
                .formatted(UUID.randomUUID(), codes);
    }

    @Override
    protected void onMessage(String message, Instant receivedAt) {
        UpbitTradeMessage msg = jsonMapper.readValue(message, UpbitTradeMessage.class);
        if (!msg.isTrade()) {
            return;   // {"status":"UP"} 등 체결 외 메시지 무시
        }
        publisher.publish(UpbitTradeMapper.toEvent(msg, receivedAt));
    }
}