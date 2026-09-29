package com.example.tradedesk.dto;

import com.example.tradedesk.domain.TradeSide;
import com.example.tradedesk.domain.TradeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TradeResponse(
        Long id,
        String ticker,
        TradeSide side,
        BigDecimal notional,
        BigDecimal rate,
        LocalDate tradeDate,
        LocalDate maturityDate,
        TradeStatus status
) {
}