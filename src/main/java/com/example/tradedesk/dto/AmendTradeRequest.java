package com.example.tradedesk.dto;

import com.example.tradedesk.domain.TradeSide;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AmendTradeRequest(
        @NotNull TradeSide side,
        @NotNull @Positive BigDecimal notional,
        @NotNull @Positive BigDecimal rate,
        @NotNull LocalDate tradeDate,
        LocalDate maturityDate
) {}