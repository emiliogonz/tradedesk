package com.example.tradedesk.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TradeResponse(
        Long id,
        String ticker,
        String side,
        BigDecimal notional,
        BigDecimal rate,
        LocalDate tradeDate,
        LocalDate maturityDate,
        String status
) {
}