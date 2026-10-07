# SpringQuant

SpringQuant는 암호화폐 캔들 데이터를 기반으로 매매 전략을 검증하는 Spring Boot 백테스트 프로젝트입니다.
실제 주문을 전송하거나 매매를 직접 수행하는 서비스가 아니라, 전략 조건과 리스크 관리 설정을 입력하면 과거 캔들 기준의 거래 결과를 계산하고 화면에서 확인할 수 있도록 만든 백테스트용 애플리케이션입니다.

## 프로젝트 목적

- 사용자는 해당 애플리케이션을 이용해 자신의 고유한 매매 전략을 테스트 할 수 있습니다.
- 지원되는 지표를 기반으로 전략을 설정 할 수 있습니다. (현재는 SuperTrend와 Bollinger Band 지표만 존재)
- 수수료, 슬리피지, 레버리지, 증거금 사용 비율을 반영해 거래 결과를 계산합니다.
- 손절, 분할 익절, 강제청산, 손절가 승격 같은 리스크 관리 규칙을 도메인 로직으로 분리합니다.
- REST API, 입력값 검증, 예외 응답, 테스트 코드를 갖추며 백테스트를 실제로 수행합니다.

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Build | Gradle Wrapper |
| API | Spring Web MVC, Validation |
| HTTP Client | Spring RestClient |
| API 문서 | springdoc-openapi |
| Test | JUnit 5, Spring Boot Test |
| Frontend | Static HTML/CSS/JavaScript, lightweight-charts |

## 주요 기능

### 백테스트 실행

- Binance Futures Kline API에서 OHLC 캔들 데이터를 조회합니다.
- 조회한 캔들을 시간순으로 순회하며 신호 판단과 체결을 분리합니다.
- 이전에 완성된 캔들로 진입/청산 신호를 판단하고, 다음 캔들의 시가 기준으로 체결합니다.
- 테스트 종료 시점에 포지션이 남아 있으면 마지막 캔들의 종가로 종료합니다.

### 전략 지표

- Bollinger Band
  - 이동평균, 표준편차, 상단선, 중심선, 하단선, 밴드폭을 계산합니다.
- SuperTrend
  - True Range, Wilder Moving Average, ATR 기반으로 추세 방향과 전환 신호를 계산합니다.
- 지원 진입 모드
  - `ST_FLIP`: SuperTrend 전환 신호 진입
  - `MA_BOUNCE`: SuperTrend 방향과 Bollinger 중심선 반등 조건 진입
  - `BB_PIERCE`: SuperTrend 방향과 Bollinger 상하단 접근 조건 진입

### 리스크 관리

- 초기 손절가 계산
- 단계별 분할 익절
- 익절 이후 손절가 승격
- 시가 갭 발생 시 손절/청산 안전 처리
- 유지증거금 기준 강제청산가 계산
- 청산가가 손절가보다 먼저 도달하는 진입을 거부하는 옵션
- 동일 SuperTrend 추세에서 1회만 진입하는 옵션

### 결과 계산

- 최종 자산
- 총 수익률
- 총 거래 수
- 승/패 거래 수
- 승률
- 총 손익
- 총 수수료
- 순손익
- MDD
- 개별 거래 내역
- 분할 청산 내역
- 이벤트 로그
- 시간별 자산 곡선

### 화면 제공

정적 페이지를 통해 백테스트 설정을 입력하고 결과를 확인할 수 있습니다.

- 캔들 차트
- Bollinger Band 라인
- SuperTrend 라인
- 진입/익절/손절/청산 마커
- 자산 곡선 차트
- 거래 내역 및 분할 청산 내역 테이블
- API 오류 메시지 표시

## 백테스트 흐름

```text
요청 수신
  -> 입력값 검증
  -> Binance Futures 캔들 조회
  -> Bollinger Band / SuperTrend 계산
  -> 캔들 순회
      -> 보유 포지션 시가 안전 청산 확인
      -> SuperTrend 반전 청산 확인
      -> 신규 진입 조건 확인
      -> 손절 / 강제청산 / 분할 익절 처리
      -> 손절가 승격 예약 및 적용
  -> 거래 결과 집계
  -> 차트 데이터와 백테스트 결과 반환
```

백테스트 엔진은 보수적인 결과를 위해 같은 캔들에서 손절과 익절이 모두 가능한 경우 손절 또는 강제청산 같은 불리한 이벤트를 먼저 처리합니다.

## API

### 백테스트 실행

```http
POST /api/backtests
Content-Type: application/json
```

아래 요청값은 UI 초기 화면의 동작 확인용 예시이며 실제 매매 설정값이 아닙니다.
종료 시각은 UI가 접속 당일 00:00으로 채우므로, 아래 예시는 2026-10-06 00:00(KST) 기준입니다.

요청 예시:

```json
{
  "symbol": "BTCUSDT",
  "interval": "1h",
  "startTimeMs": 1577804400000,
  "endTimeMs": 1791212400000,
  "initialBalance": 100,
  "feeRate": 0,
  "slippageRate": 0,
  "leverage": 1,
  "marginUsageRate": 1.0,
  "bollingerPeriod": 10,
  "bollingerMultiplier": 10.0,
  "bollingerWidthMinPercent": 0,
  "superTrendLength": 10,
  "superTrendFactor": 10.0,
  "entryMode": "ST_FLIP",
  "maTolerancePercent": 0,
  "bbTolerancePercent": 0,
  "oneEntryPerTrend": false,
  "riskManagement": {
    "stopLossPercent": 1.0,
    "takeProfitLevels": [
      { "targetPercent": 1.0, "closeRatio": 0.5 },
      { "targetPercent": 2.0, "closeRatio": 0.25 },
      { "targetPercent": 3.0, "closeRatio": 0.25 }
    ],
    "stopPromotionMode": "NONE"
  },
  "liquidation": {
    "maintenanceMarginRate": 0,
    "maintenanceAmount": 0,
    "rejectEntryIfLiquidationBeforeStop": false,
    "stopBufferPercent": 0
  }
}
```

응답은 차트 표시용 `chartPoints`와 통계/거래 내역을 담은 `result`로 구성됩니다.

```json
{
  "symbol": "BTCUSDT",
  "interval": "1h",
  "startTimeMs": 1577804400000,
  "endTimeMs": 1791212400000,
  "chartPoints": [],
  "result": {
    "initialBalance": 100.0,
    "finalBalance": 100.0,
    "totalReturnPercent": 0.0,
    "totalTrades": 0,
    "winningTrades": 0,
    "losingTrades": 0,
    "winRatePercent": 0.0,
    "totalGrossPnl": 0.0,
    "totalFee": 0.0,
    "totalNetPnl": 0.0,
    "maxDrawdownPercent": 0.0,
    "trades": [],
    "events": [],
    "equityCurve": []
  }
}
```

## 실행 방법

### 요구 사항

- Java 21
- 인터넷 연결
  - 현재 API 실행은 Binance Futures API에서 캔들을 조회합니다.

### 애플리케이션 실행

```powershell
.\gradlew.bat bootRun
```

실행 후 브라우저에서 아래 주소로 접속합니다.

```text
http://localhost:8080
```

### Swagger UI 실행

기본 프로필에서는 OpenAPI 문서가 비활성화되어 있습니다.
`backtest` 프로필로 실행하면 Swagger UI를 사용할 수 있습니다.

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=backtest'
```

```text
http://localhost:8080/swagger-ui/index.html
```

### 테스트 실행

```powershell
.\gradlew.bat test
```

## 프로젝트 구조

```text
src/main/java/io/github/devssw/springquant
|-- backtest
|   |-- api        # 백테스트 REST API 요청/응답 모델
|   |-- engine     # 백테스트 실행, 포지션, 체결가, 청산가, 리스크 처리
|   |-- model      # 백테스트 설정, 진입 모드, 손절/익절 설정
|   |-- result     # 거래 결과, 이벤트, 자산 곡선, MDD 계산
|   `-- service    # 시장 데이터 조회와 엔진 실행 조합
|-- common
|   `-- api        # 공통 오류 응답과 예외 처리
|-- indicator
|   |-- bollingerband
|   `-- supertrend
|-- market
|   `-- candledata # Binance API 클라이언트, CSV 캔들 리더
`-- strategy       # 전략 결과와 신호 모델

src/main/resources/static
|-- index.html
|-- css/backtest.css
`-- js/backtest.js
```

## 테스트 구성

현재 테스트 코드는 주요 도메인 계산과 API 계층을 중심으로 구성되어 있습니다.

- 백테스트 엔진 동작 테스트
- 체결가 계산 테스트
- 포지션 진입 검증 테스트
- 손절, 익절, 강제청산 처리 테스트
- 손절가 승격 처리 테스트
- 결과 집계, 이벤트, 자산 곡선, MDD 계산 테스트
- Bollinger Band, SuperTrend, ATR 관련 지표 계산 테스트
- Binance Futures API 클라이언트 테스트
- 백테스트 컨트롤러 및 서비스 테스트

## 현재 구현 범위

이 프로젝트는 백테스트 검증에 집중한 프로젝트입니다. 따라서 현재 범위에는 아래 기능이 포함되어 있지 않습니다.

- 실거래 주문 전송
- 거래소 API Key 기반 계정 연동
- DB 저장
- 로그인/회원 관리

결과 데이터는 현재 요청에 대한 응답으로 반환되며, 별도의 영속 저장소에 저장하지 않습니다.

## 구현 포인트

- 백테스트 엔진을 API 계층과 분리해 도메인 로직 중심으로 테스트할 수 있게 구성했습니다.
- 시장 데이터 조회는 `MarketDataClient` 인터페이스 뒤에 두어 다른 거래소나 CSV 기반 데이터 소스로 교체할 수 있게 했습니다.
- Java record와 불변 리스트 복사를 사용해 결과 객체의 의도치 않은 변경을 줄였습니다.
- 입력값 검증과 전역 예외 처리를 통해 API 오류 응답 형식을 통일했습니다.
- 손절, 익절, 청산, 손절가 승격처럼 상태 변화가 많은 로직을 작은 클래스로 분리했습니다.
- 정적 화면은 백엔드 결과를 직접 확인하기 위한 도구로 제공하며, 별도의 프론트엔드 빌드 환경 없이 실행됩니다.
