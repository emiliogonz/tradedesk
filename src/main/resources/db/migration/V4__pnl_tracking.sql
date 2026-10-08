-- Average entry rate of the open position (NULL when flat) and P&L locked in so far.
ALTER TABLE positions
    ADD COLUMN avg_rate     NUMERIC(20, 8),
    ADD COLUMN realized_pnl NUMERIC(20, 4) NOT NULL DEFAULT 0;

-- Latest end-of-day mark per instrument, set by RISK (used in Part B).
CREATE TABLE market_rates (
                              instrument_id BIGINT PRIMARY KEY REFERENCES instruments(id),
                              rate          NUMERIC(20, 8) NOT NULL,
                              marked_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
                              marked_by     VARCHAR(255)   NOT NULL
);

-- Backfill avg_rate for positions that already hold exposure.
-- Uses the volume-weighted rate of trades on the position's current side.
-- Exact when a position was only ever added to; an approximation if it was
-- partially reduced before this migration. Acceptable here because only
-- local development data predates it.
UPDATE positions p
SET avg_rate = sub.vwap
    FROM (
    SELECT instrument_id, side,
           SUM(notional * rate) / SUM(notional) AS vwap
    FROM trades
    WHERE status = 'OPEN'
    GROUP BY instrument_id, side
) sub
WHERE sub.instrument_id = p.instrument_id
  AND p.net_exposure <> 0
  AND sub.side = CASE WHEN p.net_exposure > 0 THEN 'BUY' ELSE 'SELL' END;