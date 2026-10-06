package com.example.tradedesk.service;

import com.example.tradedesk.domain.*;
import com.example.tradedesk.dto.CreateTradeRequest;
import com.example.tradedesk.dto.TradeResponse;
import com.example.tradedesk.exception.InstrumentNotFoundException;
import com.example.tradedesk.exception.NoRiskLimitException;
import com.example.tradedesk.exception.RiskLimitExceededException;
import com.example.tradedesk.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
public class TradeService {

    private final TradeRepository tradeRepository;
    private final InstrumentRepository instrumentRepository;
    private final UserRepository userRepository;
    private final PositionRepository positionRepository;
    private final RiskLimitRepository riskLimitRepository;

    public TradeService(TradeRepository tradeRepository, InstrumentRepository instrumentRepository, UserRepository userRepository,
                        PositionRepository positionRepository,
                        RiskLimitRepository riskLimitRepository){
        this.tradeRepository = tradeRepository;
        this.instrumentRepository = instrumentRepository;
        this.userRepository = userRepository;
        this.positionRepository = positionRepository;
        this.riskLimitRepository = riskLimitRepository;
    }

    @Transactional
    public TradeResponse createTrade(CreateTradeRequest request, String userEmail) {
        Instrument instrument = instrumentRepository.findById(request.instrumentId())
                .orElseThrow(() -> new InstrumentNotFoundException(request.instrumentId()));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + userEmail));

        // Lock first: everything below sees an exposure no one else can change until commit.
        Position position = positionRepository.findByIdForUpdate(instrument.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "No position row for instrument " + instrument.getId()));

        // Fail closed: no configured limit means no trading.
        RiskLimit limit = riskLimitRepository.findById(instrument.getId())
                .orElseThrow(() -> new NoRiskLimitException(instrument.getTicker()));

        BigDecimal delta = request.side() == TradeSide.BUY
                ? request.notional()
                : request.notional().negate();
        BigDecimal current = position.getNetExposure();
        BigDecimal proposed = current.add(delta);

        boolean increasesRisk = proposed.abs().compareTo(current.abs()) > 0;
        boolean overLimit = proposed.abs().compareTo(limit.getMaxExposure()) > 0;
        if (increasesRisk && overLimit) {
            throw new RiskLimitExceededException(instrument.getTicker(), proposed, limit.getMaxExposure());
        }

        Trade trade = new Trade();
        trade.setInstrument(instrument);
        trade.setUser(user);
        trade.setSide(request.side());
        trade.setNotional(request.notional());
        trade.setRate(request.rate());
        trade.setTradeDate(request.tradeDate());
        trade.setMaturityDate(request.maturityDate());
        trade.setStatus(TradeStatus.OPEN);
        Trade saved = tradeRepository.save(trade);

        position.setNetExposure(proposed);
        position.setLastUpdated(OffsetDateTime.now());

        return toResponse(saved);
    }
    @Transactional(readOnly = true)
    public Page<TradeResponse> searchTrades(Long instrumentId, LocalDate from, LocalDate to, Pageable pageable)        {
        return tradeRepository.search(instrumentId, from, to, pageable).map(this::toResponse);
    }
    private TradeResponse toResponse(Trade trade) {
        return new TradeResponse(
                trade.getId(),
                trade.getInstrument().getTicker(),
                trade.getSide(),
                trade.getNotional(),
                trade.getRate(),
                trade.getTradeDate(),
                trade.getMaturityDate(),
                trade.getStatus()
        );
    }
}