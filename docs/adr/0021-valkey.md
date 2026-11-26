# 0021. 공유 저장소로 Valkey 선택 (Redis 호환, BSD-3)

- 상태: 채택
- 날짜: 2026-11-26
- 관련: #61, ADR 0022, ADR 0023, ADR 0024

## 맥락
- premium 다중 인스턴스에 최신 시세·환율 공유 저장소가 필요했다 (ADR 0022).
- 이후 스케줄러 분산 락(ADR 0023), collector 리더 선출(ADR 0024), 3단계 실시간 캔들(Hash + Lua), gateway 요청 수 제한에서도 같은 종류의 저장소를 쓸 계획이다.
- 프로젝트 개요의 확인 필요 사항: "Redis 라이선스·버전 (Valkey 포함 검토)"

## 선택지
- A: Redis 8 (RSALv2 / SSPLv1 / AGPLv3 중 선택)
- B: Valkey (BSD-3, 리눅스 재단)

## 결정
B. Valkey 9.0을 쓴다. 클라이언트는 Redis 호환 프로토콜이라 `spring-boot-starter-data-redis`를 그대로 쓴다.

## 이유
- 우리가 쓰는 기능(문자열, Hash, Lua, `SET NX PX`)은 둘 다 같다. Redis 8만의 기능(JSON, 벡터 검색 등)은 쓰지 않는다.
- Redis 8은 AGPLv3를 포함한 라이선스로 제공된다. 수정 없이 쓰면 문제는 없지만, 금융권처럼 사내 정책으로 AGPL을 제한하는 곳이 많다.
- Redis는 한 회사가 1년 사이 라이선스를 두 번 바꿨다. Valkey는 여러 회사가 참여하는 리눅스 재단 프로젝트라 변경 위험이 작다.
- 코드가 같아서 되돌릴 비용이 작다 (docker-compose 이미지 한 줄).

## 구성
- 이미지 `valkey/valkey:9.0`: 마이너까지만 고정해 9.0.x 보안 패치는 따라가고 동작 변경은 피한다 (도입 시점 9.0.8).
- 포트 `127.0.0.1:6380` (다른 프로젝트와 겹치지 않게, 로컬에서만 접근)
- AOF(`--appendonly yes`) + 볼륨: 환율은 collector가 같은 날 다시 발행하지 않아, Valkey 재시작으로 유실되면 다음 고시까지 김프 계산이 불가하다.
- 통합 테스트: Testcontainers `GenericContainer("valkey/valkey:9.0")` + `@ServiceConnection(name = "redis")`

## 트레이드오프
- 코드에 `Redis` 이름(`StringRedisTemplate` 등)이 계속 나온다. Valkey가 Redis 호환이라 같은 도구를 쓰기 때문이다.
- 저장소 하나에 여러 용도(최신값, 락, 리더 열쇠)가 모이므로 Valkey 장애의 영향이 크다 → Toxiproxy 묶음에서 장애 대응 검증.
- 다시 볼 조건: Redis 전용 기능이 필요해지거나, 클라우드(7단계)에서 관리형 서비스 선택지가 달라질 때.
