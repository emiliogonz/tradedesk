package com.example.tradedesk.controller;

import com.example.tradedesk.dto.AmendTradeRequest;
import com.example.tradedesk.dto.CreateTradeRequest;
import com.example.tradedesk.dto.TradeResponse;
import com.example.tradedesk.service.TradeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<TradeResponse> create(
            @Valid @RequestBody CreateTradeRequest request,
            Authentication authentication
    ) {
        TradeResponse response = tradeService.createTrade(request, authentication.getName());
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
    @GetMapping("/{id}")
    public TradeResponse get(@PathVariable Long id) {
        return tradeService.getTrade(id);
    }

    @PostMapping("/{id}/cancel")
    public TradeResponse cancel(@PathVariable Long id, Authentication authentication) {
        return tradeService.cancelTrade(id, authentication.getName(), isRisk(authentication));
    }

    @PostMapping("/{id}/amend")
    public ResponseEntity<TradeResponse> amend(@PathVariable Long id,
                                               @Valid @RequestBody AmendTradeRequest request,
                                               Authentication authentication) {
        TradeResponse replacement =
                tradeService.amendTrade(id, request, authentication.getName(), isRisk(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(replacement);
    }

    private static boolean isRisk(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RISK"));
    }
}
