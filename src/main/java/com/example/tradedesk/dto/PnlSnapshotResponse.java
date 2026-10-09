package com.example.tradedesk.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PnlSnapshotResponse(
        LocalDate snapshotDate,
        Long instrumentId,
        String ticker,
        String currency,
        BigDecimal netExposure,
        BigDecimal markRate,
        BigDecimal realizedPnl,
        BigDecimal unrealizedPnl
) { }
