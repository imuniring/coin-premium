# 008. 새 PC(WSL)에서 실행이 안 됨

- 날짜: 2026-11-11

## 증상
작업 PC를 바꾼 뒤 연달아 발생
1. premium-service가 `localhost:19092`에 연결 실패 (`Node may not be available`)
2. `docker` 명령 없음
3. `ls -a | grep .env` 결과 없음
4. `git push` 시 `Username for 'https://github.com'` 입력 요구

## 원인 / 해결
| 문제 | 원인 | 해결 |
| --- | --- | --- |
| Kafka 연결 실패 | 새 PC에 인프라 컨테이너가 없음 | Docker 설치 후 `docker compose up -d` |
| Docker 없음 | WSL에 미설치 | Docker Engine 설치 (회사 PC라 Desktop 라이선스 대신 Engine 선택), `usermod -aG docker` 후 WSL 재시작 |
| `.env` 없음 | Git에 올리지 않는 파일 | `cp .env.example .env` 후 값 입력 (DB 계정은 PC마다 달라도 됨) |
| GitHub 인증 요구 | 비밀번호 인증 미지원, 새 PC 미로그인 | `gh auth login` (WSL에서는 브라우저에 코드 직접 입력) |

## 추가로 확인한 것
- 회사 프로젝트와 같은 WSL을 쓰므로 커밋 작성자가 회사 이메일로 찍히지 않게 저장소별 `git config user.email` 확인
- Kafka 클라이언트는 브로커가 없어도 앱을 종료하지 않고 재접속을 시도한다 (인프라를 띄우면 자동 연결)

## 배운 점
README의 실행 방법을 새 환경에서 그대로 따라 해보며 빠진 단계를 찾았다 → README에 "새 개발 환경 세팅" 섹션 추가
