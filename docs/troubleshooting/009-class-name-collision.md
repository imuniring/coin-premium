# 009. 같은 이름의 클래스를 잘못 import

- 날짜: 2026-11-11
- 관련: [ADR 0003](../adr/0003-jooq.md)

## 증상
테스트에서 `new PremiumSnapshot(premium, domestic, foreign, fx)`가 컴파일 에러.
```
'PremiumSnapshot(...)' has private access in 'com.uni.coinpremium.premium.jooq.tables.PremiumSnapshot'
```

## 원인
| 클래스 | 정체 |
| --- | --- |
| `premium.application.service.PremiumSnapshot` | 김프 + 원본 값 record (사용하려던 것) |
| `premium.jooq.tables.PremiumSnapshot` | jOOQ가 생성한 테이블 클래스 |

IntelliJ 자동 import가 jOOQ 생성 클래스를 선택했다.

## 해결
import를 `application.service.PremiumSnapshot`으로 수정. jOOQ 쪽은 클래스 대신 `Tables.PREMIUM_SNAPSHOT` 상수만 static import한다.

## 배운 점
- IDE의 "Make ... public" 같은 자동 수정은 원인과 무관하고, 생성 코드는 빌드마다 덮어써진다
- 이름이 같은 클래스가 생기면 import 후보의 패키지 경로를 확인한다. 반복되면 record 이름 변경을 검토
