# 010. `new BigDecimal(1340.7)`로 오차 유입

- 날짜: 2026-11-11

## 증상
테스트 데이터에 `new BigDecimal(1340.7)`(따옴표 없음)을 사용.

## 원인
double 생성자가 호출되어 이미 이진 부동소수 오차가 포함된 값이 만들어진다.
```
new BigDecimal(1340.7)   → 1340.70000000000004547473508864641189575195312500
new BigDecimal("1340.7") → 1340.7
```

## 해결
금액·환율은 항상 문자열 생성자(또는 `BigDecimal.valueOf`)로 만든다.

## 관련 규칙
- BigDecimal 값 비교는 `equals` 대신 `compareTo` (`107160000.00000000`과 `107160000`은 equals로 다름)
- 나눗셈은 자릿수·반올림 방식을 지정 (`divide(x, 10, HALF_UP)`), 무한소수에서 `ArithmeticException` 방지
