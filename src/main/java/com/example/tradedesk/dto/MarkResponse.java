package com.example.tradedesk.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MarkResponse(Long instrumentId, String ticker, BigDecimal rate,
                           OffsetDateTime markedAt, String markedBy) {}