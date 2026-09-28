package com.example.tradedesk.controller;

import com.example.tradedesk.dto.CreateTradeRequest;
import com.example.tradedesk.dto.TradeResponse;
import com.example.tradedesk.service.TradeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
