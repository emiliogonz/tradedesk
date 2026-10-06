package com.example.tradedesk.service;

import com.example.tradedesk.domain.*;
import com.example.tradedesk.dto.CreateTradeRequest;
import com.example.tradedesk.dto.TradeResponse;
import com.example.tradedesk.exception.InstrumentNotFoundException;
import com.example.tradedesk.repository.InstrumentRepository;
import com.example.tradedesk.repository.PositionRepository;
import com.example.tradedesk.repository.TradeRepository;
import com.example.tradedesk.repository.UserRepository;
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

    public TradeService(TradeRepository tradeRepository, InstrumentRepository instrumentRepository, UserRepository userRepository,
                        PositionRepository positionRepository){
        this.tradeRepository = tradeRepository;
        this.instrumentRepository = instrumentRepository;
        this.userRepository = userRepository;
        this.positionRepository = positionRepository;
    }

    @Transactional
    public TradeResponse createTrade(CreateTradeRequest request, String userEmail) {
        Instrument instrument = instrumentRepository.findById(request.instrumentId())
                .orElseThrow(() -> new InstrumentNotFoundException(request.instrumentId()));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + userEmail));

        // Lock this instrument's position row until commit.
        // A concurrent trade on the same instrument waits here.
        Position position = positionRepository.findByIdForUpdate(instrument.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "No position row for instrument " + instrument.getId()));

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

        BigDecimal delta = trade.getSide() == TradeSide.BUY
                ? trade.getNotional()
                : trade.getNotional().negate();
        position.setNetExposure(position.getNetExposure().add(delta));
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