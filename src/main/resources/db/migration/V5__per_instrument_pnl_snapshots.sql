DROP TABLE pnl_snapshots;

CREATE TABLE pnl_snapshots(
  id              BIGSERIAL      PRIMARY KEY,
  snapshot_date   DATE           NOT NULL,
  instrument_id   BIGINT         NOT NULL REFERENCES instruments(id),
  currency        VARCHAR(3)     NOT NULL,
  net_exposure    NUMERIC(20, 4) NOT NULL,
  avg_rate        NUMERIC(20, 8),
  mark_rate       NUMERIC(20, 8),
  realized_pnl    NUMERIC(20, 4) NOT NULL,
  unrealized_pnl  NUMERIC(20, 4),
  created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
  UNIQUE (snapshot_date, instrument_id)
);