package com.uni.coinpremium.collector.adapter.in.websocket.binance;

import com.uni.coinpremium.collector.application.port.out.TickerPublisher;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.net.http.WebSocket;

/**
 * Binance WebSocket 연결 관리
 */
@Component
public class BinanceWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(BinanceWebSocketClient.class);

    private final BinanceProperties properties;
    private final JsonMapper jsonMapper;
    private final TickerPublisher publisher;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private volatile WebSocket webSocket;

    public BinanceWebSocketClient(BinanceProperties properties, JsonMapper jsonMapper, TickerPublisher publisher) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.publisher = publisher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void connect() {
        log.info("[binance] 연결 시도: {}", properties.streamUri());
        httpClient.newWebSocketBuilder()
                .buildAsync(properties.streamUri(), new BinanceMessageListener(jsonMapper, publisher))
                .thenAccept(ws -> this.webSocket = ws)
                .exceptionally(e -> {
                    log.error("[binance] 연결 실패", e);
                    return null;
                });
    }

    @PreDestroy
    public void close() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
    }
}