# 017. SSL 예외 전체를 영구 실패로 분류해 연결 끊김까지 재시도 안 함

- 날짜: 2026-11-25
- 관련: #48, #50, ADR 0018

## 증상
#48에서 인증서(PKIX) 오류를 재시도 대상에서 뺀 뒤, 환율 수집이 첫 시도에서 바로 실패했다.
`일시적 실패 (1회차) ... 재시도` 로그 없이 곧바로 `환율 수집 실패`가 찍혔다.

```
ExchangeRateFetchException: koreaexim request failed: Remote host terminated the handshake
Caused by: javax.net.ssl.SSLHandshakeException: Remote host terminated the handshake
Caused by: java.net.SocketException: Connection reset
```

## 원인
분류 기준을 "원인 체인에 `SSLException`이 있으면 영구 실패"로 잡았다.
그런데 `SSLHandshakeException`은 겉모양만 같고 원인이 다른 두 상황에서 모두 발생한다.

| 상황 | 진짜 원인 | 다시 하면 |
| --- | --- | --- |
| PKIX 오류 | 인증서를 신뢰할 수 없음 (`ValidatorException`, `CertPathBuilderException`) | 같은 결과 → 포기 |
| 이번 경우 | 보안 연결 도중 네트워크가 끊김 (`SocketException`) | 될 수 있음 → 재시도 |

겉모양(예외 타입) 기준으로 분류해 일시적 네트워크 오류까지 포기했다.

## 해결
인증서 검증 실패는 원인 체인에 `java.security.GeneralSecurityException` 계열이 있다.
기준을 "SSL 예외가 있나"에서 "인증서 검증 예외가 있나"로 좁혔다.

```java
static boolean isCertificateFailure(Throwable e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
        if (t instanceof GeneralSecurityException) {
            return true;
        }
    }
    return false;
}
```

실제 발생한 모양(`SSLHandshakeException` ← `SocketException`)을 테스트로 고정했다.
`SSLHandshakeException`은 원인을 받는 생성자가 없어 `initCause()`로 만든다.

## 함께 겪은 문제: yml 속성 이름 오타가 조용히 무시됨
서킷 브레이커 설정에 `record-exception-predicate`라고 적었는데, 올바른 이름은 `record-failure-predicate`였다
(재시도는 `retry-exception-predicate`라 이름 규칙이 다르다).
Spring은 모르는 속성을 에러 없이 무시하므로, 그대로 두면 서킷 브레이커가 기본값(모든 예외 집계)으로 동작해
4xx·인증 오류까지 "서버 장애"로 셌을 것이다. IntelliJ의 알 수 없는 속성 경고로 발견했다.
→ 설정이 실제로 동작하는지 가짜 HTTP 서버 테스트(`KoreaEximResilienceTest`)로 검증한다.

## 배운 점
- 예외는 겉모양(타입)이 아니라 원인으로 분류한다. 같은 타입이 서로 다른 원인을 감쌀 수 있다.
- 테스트 데이터는 실제 발생한 모양을 따른다. 원인 없는 `SSLHandshakeException`으로만 테스트해서 문제를 놓쳤다.
- 설정 기반 동작은 기동 성공만으로 검증되지 않는다. 동작 테스트가 필요하다.
