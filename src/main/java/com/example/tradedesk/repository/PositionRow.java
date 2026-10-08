package com.example.tradedesk.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Raw position data joined from positions, instruments, marks and limits. */
public record PositionRow(
        Long instrumentId,
        String ticker,
        String currencyPair,
        BigDecimal netExposure,
        BigDecimal avgRate,
        BigDecimal realizedPnl,
        BigDecimal markRate,
        BigDecimal maxExposure,
        OffsetDateTime lastUpdated
) {}