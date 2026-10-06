package com.example.tradedesk.service;

import com.example.tradedesk.domain.Instrument;
import com.example.tradedesk.domain.Position;
import com.example.tradedesk.dto.CreateInstrumentRequest;
import com.example.tradedesk.dto.InstrumentResponse;
import com.example.tradedesk.repository.InstrumentRepository;
import com.example.tradedesk.repository.PositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;
    private final PositionRepository positionRepository;

    public InstrumentService(InstrumentRepository instrumentRepository,
                             PositionRepository positionRepository) {
        this.instrumentRepository = instrumentRepository;
        this.positionRepository = positionRepository;
    }

    @Transactional
    public InstrumentResponse create(CreateInstrumentRequest request) {
        Instrument instrument = new Instrument();
        instrument.setTicker(request.ticker());
        instrument.setType(request.type());
        instrument.setCurrencyPair(request.currencyPair());
        Instrument saved = instrumentRepository.save(instrument);

        Position position = new Position();
        position.setInstrumentId(saved.getId());
        position.setNetExposure(BigDecimal.ZERO);
        position.setLastUpdated(OffsetDateTime.now());
        positionRepository.save(position);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<InstrumentResponse> list() {
        return instrumentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private InstrumentResponse toResponse(Instrument instrument) {
        return new InstrumentResponse(
                instrument.getId(),
                instrument.getTicker(),
                instrument.getType(),
                instrument.getCurrencyPair()
        );
    }
}