package com.example.tradedesk.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTradeRequest(
        @NotNull Long instrumentId,
        @NotNull String side,
        @NotNull @Positive BigDecimal notional,
        @NotNull @Positive BigDecimal rate,
        @NotNull LocalDate tradeDate,
        LocalDate maturityDate
) {
}