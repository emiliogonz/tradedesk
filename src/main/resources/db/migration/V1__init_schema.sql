
CREATE TABLE users (
                       id            BIGSERIAL PRIMARY KEY,
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role          VARCHAR(50)  NOT NULL DEFAULT 'TRADER'
);

CREATE TABLE instruments (
                             id             BIGSERIAL PRIMARY KEY,
                             ticker         VARCHAR(50)  NOT NULL UNIQUE,
                             type           VARCHAR(50)  NOT NULL,
                             currency_pair  VARCHAR(20)  NOT NULL
);

CREATE TABLE trades (
                        id             BIGSERIAL PRIMARY KEY,
                        user_id        BIGINT NOT NULL REFERENCES users(id),
                        instrument_id  BIGINT NOT NULL REFERENCES instruments(id),
                        side           VARCHAR(10)    NOT NULL,
                        notional       NUMERIC(20, 4) NOT NULL,
                        rate           NUMERIC(20, 8) NOT NULL,
                        trade_date     DATE NOT NULL,
                        maturity_date  DATE,
                        status         VARCHAR(20) NOT NULL DEFAULT 'OPEN'
);

CREATE INDEX idx_trades_trade_date    ON trades(trade_date);
CREATE INDEX idx_trades_instrument_id ON trades(instrument_id);

CREATE TABLE positions (
                           instrument_id  BIGINT PRIMARY KEY REFERENCES instruments(id),
                           net_exposure   NUMERIC(20, 4) NOT NULL DEFAULT 0,
                           last_updated   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE pnl_snapshots (
                               id              BIGSERIAL PRIMARY KEY,
                               snapshot_date   DATE NOT NULL,
                               realized_pnl    NUMERIC(20, 4) NOT NULL DEFAULT 0,
                               unrealized_pnl  NUMERIC(20, 4) NOT NULL DEFAULT 0,
                               UNIQUE (snapshot_date)
);

CREATE TABLE risk_limits (
                             instrument_id  BIGINT PRIMARY KEY REFERENCES instruments(id),
                             max_exposure   NUMERIC(20, 4) NOT NULL
);