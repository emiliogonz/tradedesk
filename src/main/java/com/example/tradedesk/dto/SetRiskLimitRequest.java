package com.example.tradedesk.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record SetRiskLimitRequest(@NotNull @PositiveOrZero BigDecimal maxExposure) {
}
