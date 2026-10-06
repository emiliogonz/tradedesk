package com.example.tradedesk.exception;

import java.math.BigDecimal;

public class RiskLimitExceededException extends RuntimeException {
    public RiskLimitExceededException(String ticker, BigDecimal proposed, BigDecimal limit) {
        super("Trade would bring %s exposure to %s; limit is %s"
                .formatted(ticker, proposed.toPlainString(), limit.toPlainString()));
    }
}