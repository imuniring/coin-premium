# 0006. 1단계부터 WebSocket 수집, JDK HttpClient 사용

- 상태: 채택
- 날짜: 2026-11-05

## 맥락
실시간 김프와 이후 실시간 캔들을 만들려면 체결 데이터를 지연 없이 받아야 한다.

## 선택지
- 수집 방식: REST 폴링(1분) / WebSocket
- 클라이언트: JDK HttpClient WebSocket / Spring WebSocket Client / OkHttp

## 결정
- 1단계부터 WebSocket으로 수집한다. 구독 타입은 Upbit·Binance 모두 체결(trade)
- 클라이언트는 JDK 기본 `java.net.http.WebSocket`
- Binance는 구독 메시지 대신 URL(combined stream)로 구독, 시세 전용 엔드포인트(data-stream.binance.vision) 사용

## 이유
- REST 폴링은 폴링 주기만큼 지연되고 호출 제한에 걸리기 쉬움
- 체결 데이터는 `tradedAt` 의미와 일치하고, 3단계 실시간 캔들에 그대로 재사용 가능
- JDK HttpClient는 추가 의존성 0개, Virtual Threads와 궁합이 좋음
- Binance는 연결당 초당 메시지 제한(5개)이 있어 URL 구독이 재연결 시 부담이 적음

## 트레이드오프
- 저수준 API라 직접 처리해야 할 것이 많다
  - 매 메시지마다 `request(1)` 호출 (빠뜨리면 첫 메시지 후 조용히 멈춤)
  - 메시지 조각(last=false) 버퍼링, 텍스트·바이너리 프레임 모두 처리
- 연결 유지·재연결 책임이 생김 → 0009
