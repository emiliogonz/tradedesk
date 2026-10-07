package com.example.tradedesk.dto;

import java.math.BigDecimal;

public record RiskLimitResponse(Long instrumentId, String ticker, BigDecimal maxExposure) {
}
