package com.example.tradedesk.exception;

public class NoRiskLimitException extends RuntimeException {
    public NoRiskLimitException(String ticker) {
        super("No risk limit configured for " + ticker + "; trading is not permitted");
    }
}