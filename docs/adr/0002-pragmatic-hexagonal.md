# 0002. 서비스 내부 실용적 헥사고날

- 상태: 채택
- 날짜: 2026-11-05

## 맥락
로드맵 자체가 바깥 기술을 계속 교체하는 구조다. (REST → WebSocket, 로그 → Kafka, 스케줄러 → Kafka Streams, 메모리 → Redis) 교체할 때마다 핵심 로직을 고치고 싶지 않다.

## 선택지
- A: 레이어드 (controller / service / repository)
- B: 엄격한 헥사고날 (모든 기능에 포트·어댑터·매핑)
- C: 실용적 헥사고날 (핵심 도메인만 엄격, 단순 기능은 가볍게)

## 결정
C. `application/port`, `application/service`, `adapter/in`, `adapter/out`으로 나누되, 단순 조회까지 모든 계층을 강제하지 않는다.

## 이유
- 실제 효과 확인: 로그 발행기를 Kafka 발행기로 교체할 때 WebSocket 수신 코드 변경 0줄
- 포트가 작은 인터페이스라 테스트에서 람다로 가짜 구현 가능 (Mockito 없이 테스트)

## 트레이드오프
- 파일·매핑 코드가 늘어난다
- 규칙을 말로만 지키면 무너지므로 ArchUnit으로 강제 예정
