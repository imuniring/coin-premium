# 0025. 코인 레지스트리: 지원 코인과 거래소 마켓 코드를 공통 계약으로

- 상태: 채택
- 날짜: 2026-11-27
- 관련: #71, ADR 0008

## 맥락
- 지원 코인 목록이 collector(Upbit `codes`, Binance `symbols`)와 premium(`symbols`) 설정에 각각 흩어져 있었다. 코인 하나를 추가하려면 세 곳을 빠짐없이 고쳐야 했다.
- 심볼과 거래소 마켓 코드의 대응(`BTC` ↔ `KRW-BTC`, `BTCUSDT`)이 코드 어디에도 명시되지 않았다. 매퍼는 마켓 코드를 문자열 규칙으로 잘라 심볼을 만들었다.
- 지원 코인을 조회할 API가 없었다.
- 다음 묶음(공백 보정)은 심볼로 거래소 REST를 부르기 위해 마켓 코드가 필요하다.

## 선택지
| 방법 | 장점 | 단점 |
| --- | --- | --- |
| A. 공통 계약(`event-contract`)에 Java 코드로 정의 | 단순, 서비스 간 실행 중 의존 없음, 오타를 컴파일러가 잡음 | 코인 추가 시 재배포 |
| B. premium DB + collector가 시작 시 premium API 조회 | 재배포 없이 추가 | collector가 premium이 살아 있어야 기동 (장애 격리 깨짐) |
| C. premium DB + Kafka로 변경 전파 | 재배포 없이 추가, 장애 격리 유지 | 실행 중 구독 목록 변경까지 처리해야 해 현재 요구 대비 복잡 |

## 결정
A. `event-contract`의 `contract.registry` 패키지에 `Coin`, `CoinRegistry`를 Java 코드로 정의한다.

## 이유
- 코인 추가는 드물고, 추가할 때 거래소 상장 여부와 마켓 코드를 확인·테스트하게 되므로 재배포가 부담이 아니다.
- 심볼과 마켓 코드는 collector가 만들고 premium이 받는 이벤트의 전제 조건이라 서비스 간 공유 계약으로 보는 것이 자연스럽다. 서비스는 `event-contract`만 의존한다는 규칙도 그대로 유지된다.
- JSON·YAML 파일은 파싱 라이브러리가 필요해 `event-contract`의 순수 Java 원칙이 깨진다. Java 코드는 거래소를 enum으로 써서 오타를 컴파일 단계에서 막는다.
- B는 프로젝트 첫 원칙인 장애 격리를 깬다. C는 "재배포 없이 코인 추가"가 실제로 필요해지면 다시 검토한다.

## 역할 분리
| | 역할 | 바꾸는 방법 |
| --- | --- | --- |
| 레지스트리 (이번) | 어떤 코인을 지원할 수 있나 (목록, 이름, 마켓 코드) | 코드 수정 + 배포 |
| Feature Flag (7번 묶음) | 지금 수집·계산할까 (ON/OFF) | 실행 중 즉시 |

## 구성
- `Coin(symbol, name, Map<Exchange, String> markets)`: `Map.copyOf`로 불변. 거래소가 늘면 Map에 한 줄씩 추가한다.
- `CoinRegistry`: `all()`, `find(symbol)`, `marketsOf(exchange)`(구독 목록), `findByMarket(exchange, market)`(마켓 코드 → 코인)
- 심볼 중복은 클래스 로딩 시점에 실패한다 (`toUnmodifiableMap`).
- 테스트로 모든 코인이 김프 계산에 필요한 Upbit·Binance 마켓 코드를 둘 다 갖는지 보장한다.

### 서비스별 사용
- collector: 구독 목록을 `marketsOf()`로 만들고, 매퍼는 문자열 규칙 대신 `findByMarket()`으로 심볼을 찾는다. 거래소마다 이름이 다른 코인도 대응하고, 구독하지 않은 마켓 코드는 예외로 걸러 낸다.
- premium: 서비스는 `SupportedCoins`(지원 여부·심볼 목록), `CoinCatalog`(이름·마켓 코드) 포트에만 의존하고, `CoinRegistryAdapter`가 둘 다 구현한다.
  - 포트를 둔 이유: Feature Flag가 붙으면 "지원 코인"이 "레지스트리 중 켜진 것"으로 바뀌는데, 이때 어댑터 한 곳만 고치면 된다. 테스트에서 원하는 코인 목록을 넣을 수 있는 것도 이점이다.
  - 포트를 둘로 나눈 이유: 김프 서비스들은 심볼만 필요하다. 필요한 것만 담은 작은 포트라 테스트에서 람다 한 줄로 만들 수 있다.
- API: `GET /v1/coins`, `GET /v1/coins/{symbol}`. 목록은 이후 페이지네이션을 위해 객체로 감싸고, `markets`는 거래소가 늘어도 모양이 같도록 배열로, `Exchange` 정의 순서로 고정한다. 활성 여부는 Feature Flag와 함께 추가한다.

## 트레이드오프
- 코인 추가·변경은 collector와 premium을 함께 배포해야 한다.
- `Coin`은 "거래소 하나당 마켓 하나"만 담는다.
- 레지스트리를 아직 지원하지 않는 이전 버전 서비스가 섞여 돌면 지원 코인이 서로 다를 수 있다 (배포 순서 주의).

## 다시 볼 조건
- **통일 마켓 코드**: 한 거래소에 같은 코인의 마켓이 여러 개 필요해지거나(예: 테더 기준 김프의 Upbit `KRW-USDT`, Binance의 여러 호가 통화), 거래소·마켓을 입력으로 받는 API(3단계 캔들 등)가 생기면
  서비스 공통 마켓 형식(`{기초자산}-{호가통화}`, 예: `BTC-KRW`)과 `Market(exchange, base, quote, exchangeCode)` 단위 레지스트리로 확장한다.
- 재배포 없이 코인을 추가해야 하는 운영 요구가 생기면 C안(DB + Kafka 전파)을 검토한다.
