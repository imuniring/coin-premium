-- 1분 단위 김프 스냅샷 (UTC 저장)
CREATE TABLE premium_snapshot (
      id                  BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
      symbol              VARCHAR(20)    NOT NULL,
      premium_rate        DECIMAL(10, 4) NOT NULL,   -- 김프 (%), 소수 4자리
      domestic_price      DECIMAL(30, 8) NOT NULL,   -- 국내 원화 가격
      foreign_price       DECIMAL(30, 8) NOT NULL,   -- 해외 USDT 가격
      exchange_rate       DECIMAL(20, 4) NOT NULL,   -- USD→KRW
      domestic_traded_at  DATETIME(6)    NOT NULL,
      foreign_traded_at   DATETIME(6)    NOT NULL,
      fx_published_at     DATETIME(6)    NOT NULL,
      captured_at         DATETIME(6)    NOT NULL,   -- 스냅샷 기준 시각 (분 단위로 맞춤)
      CONSTRAINT uk_premium_snapshot UNIQUE (symbol, captured_at)
);