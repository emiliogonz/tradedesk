package com.example.tradedesk.exception;

public class InstrumentNotFoundException extends RuntimeException{
    public InstrumentNotFoundException(Long instrumentId){
        super("Instrument not Found: " + instrumentId);
    }
}
