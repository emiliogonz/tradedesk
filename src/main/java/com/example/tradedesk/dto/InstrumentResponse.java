package com.example.tradedesk.dto;

import com.example.tradedesk.domain.Instrument;

public record InstrumentResponse (
    Long id,
    String ticker,
    Instrument.InstrumentType type,
    String currencyPair
) {}
