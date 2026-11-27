-- 스냅샷 출처: LIVE(실시간 저장) / BACKFILL(공백 보정으로 나중에 채움)
-- 기존 줄은 모두 실시간 저장이므로 LIVE
ALTER TABLE premium_snapshot
    ADD COLUMN source VARCHAR(10) NOT NULL DEFAULT 'LIVE';

ALTER TABLE premium_snapshot
    ADD CONSTRAINT ck_premium_snapshot_source CHECK (source IN ('LIVE', 'BACKFILL'));