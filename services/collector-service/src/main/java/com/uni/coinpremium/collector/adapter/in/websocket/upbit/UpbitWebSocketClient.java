package com.uni.coinpremium.collector.adapter.in.websocket.upbit;

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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Upbit WebSocket 연결 관리
 */
@Component
public class UpbitWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(UpbitWebSocketClient.class);

    private final UpbitProperties properties;
    private final JsonMapper jsonMapper;
    private final TickerPublisher publisher;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private volatile WebSocket webSocket;

    public UpbitWebSocketClient(UpbitProperties properties, JsonMapper jsonMapper, TickerPublisher publisher) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.publisher = publisher;
    }


    // 앱이 완전히 뜬 뒤에 연결 시작
    @EventListener(ApplicationReadyEvent.class)
    public void connect() {
        log.info("[upbit] 연결 시도: {}", properties.url());
        httpClient.newWebSocketBuilder()
                .buildAsync(properties.url(),
                        new UpbitMessageListener(subscribeMessage(), jsonMapper, publisher))
                .thenAccept(ws -> this.webSocket = ws)
                .exceptionally(e -> {
                    log.error("[upbit] 연결 실패", e);
                    return null;
                });
    }

    // 앱 종료 시 정상 종료 신호를 보내고 닫기
    @PreDestroy
    public void close() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
    }

    // 예: [{"ticket":"..."},{"type":"trade","codes":["KRW-BTC","KRW-ETH"]}]
    private String subscribeMessage() {
        String codes = properties.codes().stream()
                .map(code -> "\"" + code + "\"")
                .collect(Collectors.joining(","));
        return "[{\"ticket\":\"%s\"},{\"type\":\"trade\",\"codes\":[%s]}]"
                .formatted(UUID.randomUUID(), codes);
    }
}