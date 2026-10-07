package com.example.tradedesk.service;

import com.example.tradedesk.domain.Instrument;
import com.example.tradedesk.domain.RiskLimit;
import com.example.tradedesk.dto.RiskLimitResponse;
import com.example.tradedesk.exception.InstrumentNotFoundException;
import com.example.tradedesk.repository.InstrumentRepository;
import com.example.tradedesk.repository.RiskLimitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class RiskLimitService {

    private final RiskLimitRepository riskLimitRepository;
    private final InstrumentRepository instrumentRepository;

    public RiskLimitService(RiskLimitRepository riskLimitRepository,
                            InstrumentRepository instrumentRepository) {
        this.riskLimitRepository = riskLimitRepository;
        this.instrumentRepository = instrumentRepository;
    }

    @PreAuthorize("hasRole('RISK')")
    @Transactional
    public RiskLimitResponse setLimit(Long instrumentId, BigDecimal maxExposure) {
        Instrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new InstrumentNotFoundException(instrumentId));

        RiskLimit limit = riskLimitRepository.findById(instrumentId).orElseGet(() -> {
            RiskLimit created = new RiskLimit();
            created.setInstrumentId(instrumentId);
            return created;
        });

        BigDecimal previous = limit.getMaxExposure();   // null if this is the first limit
        limit.setMaxExposure(maxExposure);
        riskLimitRepository.save(limit);

        String changedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Risk limit for {} changed from {} to {} by {}",
                instrument.getTicker(),
                previous == null ? "none" : previous.toPlainString(),
                maxExposure.toPlainString(),
                changedBy);

        return new RiskLimitResponse(instrumentId, instrument.getTicker(), maxExposure);
    }
}