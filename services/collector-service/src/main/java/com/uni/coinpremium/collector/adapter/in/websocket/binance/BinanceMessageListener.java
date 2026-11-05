package com.uni.coinpremium.collector.adapter.in.websocket.binance;

import com.uni.coinpremium.collector.application.port.out.TickerPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.CompletionStage;

/**
 * Binance WebSocket 메시지 수신
 */
class BinanceMessageListener implements WebSocket.Listener {

    private static final Logger log = LoggerFactory.getLogger(BinanceMessageListener.class);

    private final JsonMapper jsonMapper;
    private final TickerPublisher publisher;
    private final StringBuilder textBuffer = new StringBuilder();

    BinanceMessageListener(JsonMapper jsonMapper, TickerPublisher publisher) {
        this.jsonMapper = jsonMapper;
        this.publisher = publisher;
    }

    @Override
    public void onOpen(WebSocket ws) {
        log.info("[binance] 연결 성공");   // URL로 구독했으니 보낼 메시지 없음
        ws.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
        textBuffer.append(data);
        if (last) {
            handle(textBuffer.toString());
            textBuffer.setLength(0);
        }
        ws.request(1);
        return null;
    }

    // Binance는 20초마다 ping을 보냄 → 응답(pong)이 잘 가는지 확인용 로그
    @Override
    public CompletionStage<?> onPing(WebSocket ws, ByteBuffer message) {
        log.debug("[binance] ping 수신");
        ws.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
        log.warn("[binance] 연결 종료 - code={}, reason={}", statusCode, reason);
        return null;
    }

    @Override
    public void onError(WebSocket ws, Throwable error) {
        log.error("[binance] 연결 에러", error);
    }

    private void handle(String message) {
        Instant receivedAt = Instant.now();
        try {
            BinanceStreamMessage msg = jsonMapper.readValue(message, BinanceStreamMessage.class);
            if (msg.data() == null || !msg.data().isTrade()) {
                return;
            }
            publisher.publish(BinanceTradeMapper.toEvent(msg.data(), receivedAt));
        } catch (Exception e) {
            log.warn("[binance] 메시지 변환 실패 - message={}", message, e);
        }
    }
}