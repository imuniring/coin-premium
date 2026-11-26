# 002. 다중 인스턴스: premium 공유 저장소·스케줄러 락·collector 리더 선출

- 날짜: 2026-11-26
- 관련: #61, #63, #65, ADR 0022, ADR 0023, ADR 0024

## 요약
| 실험 | Before | After |
| --- | --- | --- |
| premium 2대 김프 조회 (2대 × 2코인) | 4개 모두 503 | 4개 모두 정상 |
| premium 2대 스냅샷 스케줄러 | 2대 모두 실행 (1대는 계산 후 버려짐) | 1대만 실행, 다른 1대는 즉시 건너뜀 |
| collector 2대 Kafka 적재 | 같은 체결 2번 | 1번 |
| collector 리더 정상 종료 | - | 즉시 인계, 연결 약 0.2초 |
| collector 리더 강제 종료(`kill -9`) | - | 약 13.8초 후 인계 |

## 환경
- 로컬, 같은 컴퓨터에서 jar로 2대 실행 (premium 8081·8083, collector 8082·8084)
- Kafka 단일 브로커, 시세 토픽 파티션 3개, Valkey 9.0.8

## 1. premium 김프 조회 (#61)

### Before (메모리 저장소)
```
== 8081 BTC ["UPBIT","BINANCE"]
== 8081 ETH ["UPBIT","BINANCE"]
== 8083 BTC ["FX_RATE"]
== 8083 ETH ["FX_RATE"]
```
같은 컨슈머 그룹이 파티션을 나눠 가져 8081은 환율만, 8083은 시세만 보유했다.

### After (Valkey)
```
== 8081 BTC 1.4899
== 8081 ETH 1.5986
== 8083 BTC 1.4900
== 8083 ETH 1.5999
```
같은 코인의 값 차이는 두 요청 사이 시세 변화다. Valkey 키: `quote:UPBIT:BTC`, `quote:UPBIT:ETH`, `quote:BINANCE:BTC`, `quote:BINANCE:ETH`, `fx:USD-KRW`.

## 2. premium 스냅샷 스케줄러 (#63)

### Before
```
A 09:55:00.019 [snapshot] 저장 - symbol=BTC / ETH
B 09:55:00.335 [snapshot] 이미 저장된 시점 - symbol=BTC / ETH
```

### After (ShedLock, lockAtMostFor 50s, lockAtLeastFor 30s)
```
A 10:14:00.052 Locked 'premium-snapshot', lock will be held at most until 01:14:50.009Z
A 10:14:00.393 [snapshot] 저장 - symbol=BTC
A 10:14:00.411 [snapshot] 저장 - symbol=ETH
A 10:14:00.417 Task finished, lock 'premium-snapshot' will be released at 01:14:30.009Z
B 10:14:01.325 Not executing 'premium-snapshot'. It's locked.
```
작업은 약 0.4초에 끝났고 B는 1.3초 늦게 깨어났다. `lockAtLeastFor`가 없었다면 B가 같은 회차를 다시 실행했다.

## 3. collector 중복 적재 (#65)

### Before (2대 모두 수집)
| offset | eventId | receivedAt |
| --- | --- | --- |
| 2278914 | BINANCE-BTCUSDT-6914105886 | 01:21:47.239158986Z |
| 2278915 | BINANCE-BTCUSDT-6914105886 | 01:21:47.240483055Z |

수신 건수: 8082 = 8,294, 8084 = 7,640 (띄운 시각 차이만큼만 다름)

### After (리더 선출)
- `GET leader:collector` = `local-baf7e3bf`, `TTL` = 14 (5초마다 15초로 연장)
- 수신 건수: 리더 2,289, 대기조 없음

## 4. collector 리더 인계 (#65)

### 정상 종료 (Ctrl+C, 열쇠 반납)
```
14:55:52.990 [leader] 리더 획득
14:55:53.118 [upbit] 연결 성공
14:55:53.199 [binance] 연결 성공
```

### 강제 종료 (kill -9, 반납 없음)
```
14:59:52.370 리더 kill -9
15:00:05.919 [leader] 리더 획득 (대기조)
15:00:06.070 [upbit] 연결 성공
15:00:06.148 [binance] 연결 성공
```
인계까지 약 13.8초. 마지막 연장부터 기한 15초가 세지므로 15초보다 짧을 수 있다.

### 공백 기록
```
16:42:05.570 [leader] 리더 교대 공백 - 07:41:48.954Z ~ 07:42:05.555Z (16601ms), 공백 보정 대상
16:42:05.974 [upbit] 연결 성공
16:42:06.009 [binance] 연결 성공
```
기록된 시작은 옛 리더의 마지막 연장 시각이라 실제보다 최대 5초 이르고, 끝은 열쇠 획득 시각이라 실제 연결 완료보다 약 0.4초 이르다.

## 관찰
- Binance 체결의 `receivedAt`이 `tradedAt`보다 약 40ms 이르게 기록됐다 (서버와 로컬 시계 차이). 수신 지연 메트릭을 만들 때 고려해야 한다.
