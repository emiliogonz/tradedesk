package com.example.tradedesk.controller;

import com.example.tradedesk.dto.RiskLimitResponse;
import com.example.tradedesk.dto.SetRiskLimitRequest;
import com.example.tradedesk.service.RiskLimitService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk-limits")
public class RiskLimitController {

    private final RiskLimitService riskLimitService;

    public RiskLimitController(RiskLimitService riskLimitService) {
        this.riskLimitService = riskLimitService;
    }

    @PutMapping("/{instrumentId}")
    public RiskLimitResponse setLimit(@PathVariable Long instrumentId,
                                      @Valid @RequestBody SetRiskLimitRequest request) {
        return riskLimitService.setLimit(instrumentId, request.maxExposure());
    }
    @GetMapping
    public List<RiskLimitResponse> list() {
        return riskLimitService.listLimits();
    }
}