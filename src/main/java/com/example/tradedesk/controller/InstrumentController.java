package com.example.tradedesk.controller;

import com.example.tradedesk.dto.CreateInstrumentRequest;
import com.example.tradedesk.dto.InstrumentResponse;
import com.example.tradedesk.service.InstrumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {
    private final InstrumentService instrumentService;
    public InstrumentController(InstrumentService instrumentService){
        this.instrumentService = instrumentService;
    }
    @PostMapping
    public ResponseEntity<InstrumentResponse> create(@Valid @RequestBody CreateInstrumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(instrumentService.create(request));
    }
    @GetMapping
    public List<InstrumentResponse> list() {
        return instrumentService.list();
    }
}

