# 0015. 에러 응답 설계 (ProblemDetail + code)

- 상태: 채택
- 날짜: 2026-11-11

## 맥락
에러마다 응답 모양이 달랐고(`ResponseStatusException`과 직접 만든 ProblemDetail 혼재), 메시지에 사용자 입력을 그대로 되돌려주고 있었다. 클라이언트가 에러를 기계적으로 구분할 방법도 없었다.

## 결정
- 모든 에러는 ProblemDetail(RFC 9457) + 고정 식별자 `code`
- `ErrorCode` enum이 code와 HTTP 상태를 관리, `PremiumExceptionHandler`의 공통 메서드로 응답 생성

| code | status | 상황 |
| --- | --- | --- |
| `INVALID_SYMBOL` | 400 | 경로 심볼 형식 오류 (영문 대문자·숫자 1~20자) |
| `INVALID_INTERVAL` | 400 | interval 값 오류 |
| `INVALID_RANGE` | 400 | from ≥ to, 최대 기간 초과 |
| `INVALID_PARAMETER` | 400 | 파라미터 타입 오류 (from·to 형식) |
| `UNSUPPORTED_SYMBOL` | 404 | 수집하지 않는 코인 |
| `MARKET_DATA_NOT_READY` | 503 | 시세·환율 준비 전 (+ `Retry-After`, `missing`) |

- `type`은 표준 기본값 `about:blank`, `title`은 HTTP 상태 문구
- 형식 검증 전 사용자 입력은 `detail`에 넣지 않는다 (검증을 통과한 값은 허용)
- 형식 오류(400)와 미지원(404)을 분리: 형식은 웹 어댑터, 지원 여부는 서비스에서 판단

## 이유
- 클라이언트는 바뀔 수 있는 문구가 아니라 고정 `code`로 분기해야 한다
- 사용자 입력을 그대로 반사하면 응답을 화면에 표시하는 클라이언트에서 XSS, 로그 오염 위험
- 400(요청을 고쳐야 함) / 404(기다려도 없음) / 503(잠시 후 재시도)을 구분해 클라이언트가 다음 행동을 정할 수 있게 함

## 대안
- `type`에 에러 문서 URI 연결: 표준이 권장하는 방식이지만 문서 주소를 계속 관리해야 하고, 외부 공개 API가 아니어서 이점이 작음 → 에러 코드 설명은 Mintlify "에러 코드" 페이지에서 제공

## 트레이드오프
- 다른 서비스(gateway 등)가 생기면 같은 형식을 공통 모듈로 추출할지 검토
