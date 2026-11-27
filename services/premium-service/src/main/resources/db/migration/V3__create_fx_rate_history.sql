-- 환율 이력 (공백 보정 시 그 시각에 적용되던 환율 조회용, UTC 저장)
CREATE TABLE fx_rate_history (
                                 id            BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                 rate          DECIMAL(20, 4) NOT NULL,   -- USD→KRW
                                 published_at  DATETIME(6)    NOT NULL,   -- 고시 기준 시각
                                 event_id      VARCHAR(50)    NOT NULL,   -- 원천 이벤트 ID (예: KOREAEXIM-USD-KRW-20261127)
                                 CONSTRAINT uk_fx_rate_history_published UNIQUE (published_at)
);