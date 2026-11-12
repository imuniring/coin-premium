# 014. 경로 변수를 `@RequestParam`으로 받음

- 날짜: 2026-11-11

## 증상
`@RequestMapping("/v1/coins/{symbol}/premium")`으로 바꾼 뒤 메서드 파라미터를 `@RequestParam String symbol`로 남겨둠.
→ `/v1/coins/BTC/premium` 호출 시 "symbol 파라미터 없음" 400 발생 예정 (리뷰 중 발견)

## 원인
| 어노테이션 | 값을 찾는 곳 |
| --- | --- |
| `@RequestParam` | 쿼리 문자열 `?symbol=BTC` |
| `@PathVariable` | 경로 템플릿 `{symbol}` |

## 해결
`@PathVariable String symbol`로 변경.

## 배운 점
경로를 바꿀 때는 매핑과 파라미터 어노테이션을 함께 확인하고, curl로 정상·오류 케이스를 모두 호출해 본다.
