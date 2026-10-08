package com.example.tradedesk.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PositionResponse(
        Long instrumentId,
        String ticker,
        String pnlCurrency,
        BigDecimal netExposure,
        BigDecimal maxExposure,
        BigDecimal avgRate,
        BigDecimal markRate,
        BigDecimal realizedPnl,
        BigDecimal unrealizedPnl,
        OffsetDateTime lastUpdated
) {}