package com.example.tradedesk.exception;

public class TradeNotFoundException extends RuntimeException {
    public TradeNotFoundException(Long tradeId) {
        super("Trade not found: " + tradeId);
    }
}