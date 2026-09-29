package com.example.tradedesk.controller;

import com.example.tradedesk.domain.Instrument;
import com.example.tradedesk.dto.CreateInstrumentRequest;
import com.example.tradedesk.repository.InstrumentRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {
    private final InstrumentRepository instrumentRepository;
    public InstrumentController(InstrumentRepository instrumentRepository){
        this.instrumentRepository = instrumentRepository;
    }
    @PostMapping
    public ResponseEntity<Instrument> create(@Valid @RequestBody CreateInstrumentRequest request){
        Instrument instrument = new Instrument();
        instrument.setTicker(request.ticker());
        instrument.setType(request.type());
        instrument.setCurrencyPair((request.currencyPair()));
        return ResponseEntity.status(HttpStatus.CREATED).body(instrumentRepository.save(instrument));
    }
    @GetMapping
    public List<Instrument> list() {
        return instrumentRepository.findAll();
    }
}
