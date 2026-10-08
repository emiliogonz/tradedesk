package com.example.tradedesk.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SetMarkRequest(@NotNull @Positive BigDecimal rate) {}