package com.example.tradedesk.service;

import com.example.tradedesk.domain.Instrument;
import com.example.tradedesk.domain.MarketRate;
import com.example.tradedesk.dto.MarkResponse;
import com.example.tradedesk.exception.InstrumentNotFoundException;
import com.example.tradedesk.repository.InstrumentRepository;
import com.example.tradedesk.repository.MarketRateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Slf4j
@Service
public class MarkService {

    private final MarketRateRepository marketRateRepository;
    private final InstrumentRepository instrumentRepository;

    public MarkService(MarketRateRepository marketRateRepository,
                       InstrumentRepository instrumentRepository) {
        this.marketRateRepository = marketRateRepository;
        this.instrumentRepository = instrumentRepository;
    }

    @PreAuthorize("hasRole('RISK')")
    @Transactional
    public MarkResponse setMark(Long instrumentId, BigDecimal rate) {
        Instrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new InstrumentNotFoundException(instrumentId));

        MarketRate mark = marketRateRepository.findById(instrumentId).orElseGet(() -> {
            MarketRate created = new MarketRate();
            created.setInstrumentId(instrumentId);
            return created;
        });

        String markedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        mark.setRate(rate);
        mark.setMarkedAt(OffsetDateTime.now());
        mark.setMarkedBy(markedBy);
        marketRateRepository.save(mark);

        log.info("Mark for {} set to {} by {}", instrument.getTicker(), rate.toPlainString(), markedBy);

        return new MarkResponse(instrumentId, instrument.getTicker(), rate, mark.getMarkedAt(), markedBy);
    }
}