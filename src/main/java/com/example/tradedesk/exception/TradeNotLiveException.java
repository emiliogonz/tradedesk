package com.example.tradedesk.exception;

import com.example.tradedesk.domain.TradeStatus;

public class TradeNotLiveException extends RuntimeException {
    public TradeNotLiveException(Long tradeId, TradeStatus status) {
        super("Trade %d is %s and can no longer be changed".formatted(tradeId, status));
    }
}