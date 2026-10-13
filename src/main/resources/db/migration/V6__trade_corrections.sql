ALTER TABLE trades
    ADD COLUMN amends_trade_id BIGINT REFERENCES trades(id),
    ADD COLUMN origin_trade_id BIGINT REFERENCES trades(id),
    ADD COLUMN corrected_at    TIMESTAMPTZ,
    ADD COLUMN corrected_by    VARCHAR(255);

ALTER TABLE trades
    ADD CONSTRAINT chk_trades_status CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED', 'AMENDED'));

CREATE INDEX idx_trades_origin_trade_id ON trades(origin_trade_id);