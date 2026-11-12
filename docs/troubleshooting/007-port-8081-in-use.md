# 007. premium-service 기동 시 8081 포트 사용 중

- 날짜: 2026-11-11

## 증상
```
Web server failed to start. Port 8081 was already in use.
```

## 원인 확인
```bash
lsof -i :8081
```
| 프로세스 | 상태 | 판단 |
| --- | --- | --- |
| Expo Go | 다른 기기의 8081로 나가는 연결 (SYN_SENT) | 무관 |
| java (PID 72517) | `*:8081 (LISTEN)` | 원인 |

`ps -p 72517 -o command`로 이전에 실행한 premium-service임을 확인한 뒤 종료했다. (`sunproxyadmin`은 8081의 서비스 이름표일 뿐)

## 해결
```bash
kill 72517
```

## 배운 점
- `LISTEN` 상태인 프로세스가 포트 점유자다. 나가는 연결(`->`)은 무관하다
- 종료 전에 `ps`로 우리 앱인지 확인한다. 다른 프로젝트 프로세스라면 끄지 않고 포트를 바꾼다
- Spring Boot는 DB 연결 → Flyway → 웹 서버 순으로 기동하므로, 웹 서버에서 실패해도 마이그레이션은 이미 적용됐을 수 있다
