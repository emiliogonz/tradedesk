package com.example.tradedesk.dto;

import com.example.tradedesk.domain.Instrument.InstrumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInstrumentRequest (
        @NotBlank String ticker,
        @NotNull InstrumentType type,
        @NotBlank String currencyPair
){}
