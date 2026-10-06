package com.example.tradedesk.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PositionResponse(
        Long instrumentId,
        String ticker,
        BigDecimal netExposure,
        OffsetDateTime lastUpdated
) {}