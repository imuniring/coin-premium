# 001. Kafka 장애 시 시세 수신: 보관함 도입 전후

- 날짜: 2026-11-25
- 관련: #42, ADR 0020, 트러블슈팅 018

## 요약
| 항목 | Before | After |
| --- | --- | --- |
| 중단 중 수신 (10초당) | +0 (60초에 1건) | +592 ~ +4,224 |
| 받았지만 발행 실패 | 44건 유실 | 0건 (`failure` 33은 재시도 횟수) |
| 수신하지 못한 체결 | 약 19,000건 추정 | 0건 |
| 복구 후 | 네트워크 버퍼의 4,661건이 몰림 | 보관함 30,125건이 10초 안에 비워짐 |
| 넘침(`dropped`) | - | 0건 |

## 환경과 방법
- 로컬 데스크톱, collector 1개, Kafka 단일 브로커(docker compose)
- 수집 대상: Upbit KRW-BTC·KRW-ETH, Binance BTCUSDT·ETHUSDT (아래 수치는 Binance만 필터)
- Actuator 메트릭을 10초 간격으로 조회
  - `collector.ticker.received{exchange=BINANCE}`: 받은 체결 수
  - `collector.ticker.published{exchange=BINANCE, result=success|failure}`: 전송 결과
  - (After) `collector.ticker.dropped`, `collector.ticker.buffer.size`
- 순서: 정상 약 1분 → `docker compose stop kafka` → 약 3분 → `docker compose start kafka` → 약 1분

## 해석 시 주의
- 체결량은 시장 상황에 따라 크게 변한다. 미수신 건수는 정상 구간 평균으로 추정한 값이다.
- Before의 `failure`는 유실 건수, After의 `failure`는 전송 시도 실패 횟수(재시도마다 증가)다.
- After의 `buffer`는 Upbit를 포함한 전체 건수라 `received - success`(Binance만)보다 조금 크다.

## Before 분석
- 정상: 10초당 +395 ~ +2,174 (평균 초당 약 110건)
- 16:08:42 중단 직후 `received - success` = 42 → 이미 프로듀서 버퍼에 있던 체결
- 60초마다 `received` +1, `failure` +1 → 메시지 하나가 `max.block.ms`(60초) 동안 블로킹 후 실패
- 16:10:43 `failure` 1 → 43: 버퍼의 42건이 `delivery.timeout.ms`(2분) 경과로 일괄 실패
- 16:12:34 복구 직후 +4,661: 운영체제 네트워크 버퍼에 쌓여 있던 메시지

## After 분석
- 중단 중 `success`는 25,287에서 멈추고 `received`는 계속 증가, 그 차이만큼 `buffer` 증가
- `failure`가 약 6초마다 증가 (전송 대기 5초 + 재시도 간격 1초)
- 16:35:38 복구 후 10초 안에 `buffer` 0, 이후 `success` = `received`
- 16:36:48 급등락으로 10초당 +8,943 (초당 약 900건) 관측 → 이때 보관함 50,000건의 버팀 시간은 약 1분

## 원본 데이터

### Before
```
16:07:31 received=10853.0 (+10853.0) success=10853.0 failure=
16:07:41 received=12318.0 (+1465.0) success=12318.0 failure=
16:07:51 received=14073.0 (+1755.0) success=14073.0 failure=
16:08:02 received=16247.0 (+2174.0) success=16247.0 failure=
16:08:12 received=17201.0 (+954.0) success=17201.0 failure=
16:08:22 received=17596.0 (+395.0) success=17596.0 failure=
16:08:32 received=18188.0 (+592.0) success=18188.0 failure=
16:08:42 received=18768.0 (+580.0) success=18726.0 failure=
16:08:52 received=18768.0 (+0) success=18726.0 failure=
16:09:02 received=18768.0 (+0) success=18726.0 failure=
16:09:12 received=18768.0 (+0) success=18726.0 failure=
16:09:22 received=18768.0 (+0) success=18726.0 failure=
16:09:32 received=18768.0 (+0) success=18726.0 failure=
16:09:42 received=18769.0 (+1.0) success=18726.0 failure=1.0
16:09:52 received=18769.0 (+0) success=18726.0 failure=1.0
16:10:02 received=18769.0 (+0) success=18726.0 failure=1.0
16:10:13 received=18769.0 (+0) success=18726.0 failure=1.0
16:10:23 received=18769.0 (+0) success=18726.0 failure=1.0
16:10:33 received=18769.0 (+0) success=18726.0 failure=1.0
16:10:43 received=18770.0 (+1.0) success=18726.0 failure=43.0
16:10:53 received=18770.0 (+0) success=18726.0 failure=43.0
16:11:03 received=18770.0 (+0) success=18726.0 failure=43.0
16:11:13 received=18770.0 (+0) success=18726.0 failure=43.0
16:11:23 received=18770.0 (+0) success=18726.0 failure=43.0
16:11:33 received=18770.0 (+0) success=18726.0 failure=43.0
16:11:43 received=18771.0 (+1.0) success=18726.0 failure=44.0
16:11:53 received=18771.0 (+0) success=18726.0 failure=44.0
16:12:03 received=18771.0 (+0) success=18726.0 failure=44.0
16:12:13 received=18771.0 (+0) success=18726.0 failure=44.0
16:12:24 received=18771.0 (+0) success=18726.0 failure=44.0
16:12:34 received=23432.0 (+4661.0) success=23388.0 failure=44.0
16:12:44 received=24044.0 (+612.0) success=24000.0 failure=44.0
16:12:54 received=24373.0 (+329.0) success=24329.0 failure=44.0
16:13:04 received=25172.0 (+799.0) success=25128.0 failure=44.0
16:13:14 received=25681.0 (+509.0) success=25637.0 failure=44.0
16:13:24 received=26321.0 (+640.0) success=26277.0 failure=44.0
```

### After
```
16:31:05 received=18217.0 (+18217.0) success=18217.0 failure= dropped= buffer=0.0
16:31:15 received=19233.0 (+1016.0) success=19233.0 failure= dropped= buffer=0.0
16:31:25 received=19919.0 (+686.0) success=19919.0 failure= dropped= buffer=0.0
16:31:35 received=20713.0 (+794.0) success=20713.0 failure= dropped= buffer=0.0
16:31:45 received=21315.0 (+602.0) success=21315.0 failure= dropped= buffer=0.0
16:31:55 received=22514.0 (+1199.0) success=22514.0 failure= dropped= buffer=0.0
16:32:05 received=23852.0 (+1338.0) success=23852.0 failure= dropped= buffer=0.0
16:32:16 received=26408.0 (+2556.0) success=25287.0 failure= dropped= buffer=1118.0
16:32:26 received=27000.0 (+592.0) success=25287.0 failure=2.0 dropped= buffer=1710.0
16:32:36 received=31224.0 (+4224.0) success=25287.0 failure=4.0 dropped= buffer=5950.0
16:32:46 received=33161.0 (+1937.0) success=25287.0 failure=5.0 dropped= buffer=7942.0
16:32:56 received=33807.0 (+646.0) success=25287.0 failure=7.0 dropped= buffer=8606.0
16:33:06 received=35415.0 (+1608.0) success=25287.0 failure=9.0 dropped= buffer=10258.0
16:33:16 received=37152.0 (+1737.0) success=25287.0 failure=10.0 dropped= buffer=12025.0
16:33:26 received=38245.0 (+1093.0) success=25287.0 failure=12.0 dropped= buffer=13130.0
16:33:36 received=39262.0 (+1017.0) success=25287.0 failure=14.0 dropped= buffer=14153.0
16:33:47 received=40076.0 (+814.0) success=25287.0 failure=16.0 dropped= buffer=14972.0
16:33:57 received=41203.0 (+1127.0) success=25287.0 failure=17.0 dropped= buffer=16216.0
16:34:07 received=41844.0 (+641.0) success=25287.0 failure=19.0 dropped= buffer=16914.0
16:34:17 received=44585.0 (+2741.0) success=25287.0 failure=21.0 dropped= buffer=19734.0
16:34:27 received=46256.0 (+1671.0) success=25287.0 failure=22.0 dropped= buffer=21466.0
16:34:37 received=47457.0 (+1201.0) success=25287.0 failure=24.0 dropped= buffer=22674.0
16:34:47 received=48454.0 (+997.0) success=25287.0 failure=26.0 dropped= buffer=23677.0
16:34:57 received=49936.0 (+1482.0) success=25287.0 failure=27.0 dropped= buffer=25181.0
16:35:07 received=51778.0 (+1842.0) success=25287.0 failure=29.0 dropped= buffer=27109.0
16:35:18 received=52796.0 (+1018.0) success=25287.0 failure=31.0 dropped= buffer=28090.0
16:35:28 received=54824.0 (+2028.0) success=25287.0 failure=32.0 dropped= buffer=30125.0
16:35:38 received=55881.0 (+1057.0) success=55881.0 failure=33.0 dropped= buffer=0.0
16:35:48 received=56703.0 (+822.0) success=56703.0 failure=33.0 dropped= buffer=0.0
16:35:58 received=58055.0 (+1352.0) success=58055.0 failure=33.0 dropped= buffer=0.0
16:36:08 received=58994.0 (+939.0) success=58994.0 failure=33.0 dropped= buffer=0.0
16:36:18 received=61210.0 (+2216.0) success=61210.0 failure=33.0 dropped= buffer=0.0
16:36:28 received=62426.0 (+1216.0) success=62426.0 failure=33.0 dropped= buffer=0.0
16:36:38 received=67688.0 (+5262.0) success=67688.0 failure=33.0 dropped= buffer=0.0
16:36:48 received=76631.0 (+8943.0) success=76633.0 failure=33.0 dropped= buffer=0.0
```
