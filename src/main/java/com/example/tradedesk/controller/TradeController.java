package com.example.tradedesk.controller;

import com.example.tradedesk.dto.CreateTradeRequest;
import com.example.tradedesk.dto.TradeResponse;
import com.example.tradedesk.service.TradeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/trades")
public class TradeController {
    private final TradeService tradeService;

    public TradeController(TradeService tradeService){
        this.tradeService = tradeService;
    }

    @PostMapping
    public ResponseEntity<TradeResponse> createTrade(@Valid @RequestBody CreateTradeRequest request){
        TradeResponse response = tradeService.createTrade(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping
    public Page<TradeResponse> search(
            @RequestParam(required = false) Long instrumentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable
    ) {
        return tradeService.searchTrades(instrumentId, from, to, pageable);
    }
}
