package com.uni.coinpremium.collector.adapter.in.websocket.upbit;

import com.uni.coinpremium.collector.application.port.out.TickerPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CompletionStage;

/**
 * Upbit WebSocket 메시지 수신
 * - 콜백은 한 번에 하나씩 순서대로 호출되므로 버퍼를 동기화 없이 써도 안전
 */
class UpbitMessageListener implements WebSocket.Listener {

    private static final Logger log = LoggerFactory.getLogger(UpbitMessageListener.class);

    private final String subscribeMessage;
    private final JsonMapper jsonMapper;          // tools.jackson.databind.json.JsonMapper
    private final TickerPublisher publisher;

    private final StringBuilder textBuffer = new StringBuilder();
    private final ByteArrayOutputStream binaryBuffer = new ByteArrayOutputStream();

    UpbitMessageListener(String subscribeMessage, JsonMapper jsonMapper, TickerPublisher publisher) {
        this.subscribeMessage = subscribeMessage;
        this.jsonMapper = jsonMapper;
        this.publisher = publisher;
    }

    @Override
    public void onOpen(WebSocket ws) {
        log.info("[upbit] 연결 성공, 구독 요청: {}", subscribeMessage);
        ws.sendText(subscribeMessage, true);
        ws.request(1);   // 다음 메시지 1개 받을 준비
    }

    // 텍스트 프레임 수신 (메시지가 여러 조각으로 올 수 있어서 last=true일 때 완성)
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

    // 바이너리 프레임 수신 (Upbit은 JSON을 바이너리로 보내는 경우가 있음)
    @Override
    public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
        byte[] bytes = new byte[data.remaining()];
        data.get(bytes);
        binaryBuffer.writeBytes(bytes);
        if (last) {
            handle(binaryBuffer.toString(StandardCharsets.UTF_8));
            binaryBuffer.reset();
        }
        ws.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
        log.warn("[upbit] 연결 종료 - code={}, reason={}", statusCode, reason);
        return null;
    }

    @Override
    public void onError(WebSocket ws, Throwable error) {
        log.error("[upbit] 연결 에러", error);
    }

    private void handle(String message) {
        Instant receivedAt = Instant.now();   // 받은 순간 기록
        try {
            UpbitTradeMessage msg = jsonMapper.readValue(message, UpbitTradeMessage.class);
            if (!msg.isTrade()) {
                return;   // {"status":"UP"} 같은 체결 외 메시지는 무시
            }
            publisher.publish(UpbitTradeMapper.toEvent(msg, receivedAt));
        } catch (Exception e) {
            // 불량 메시지 1건은 버리고, 연결은 계속 유지
            log.warn("[upbit] 메시지 변환 실패 - message={}", message, e);
        }
    }
}