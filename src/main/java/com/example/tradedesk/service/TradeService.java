package com.example.tradedesk.service;

import com.example.tradedesk.domain.Instrument;
import com.example.tradedesk.domain.Trade;
import com.example.tradedesk.domain.User;
import com.example.tradedesk.dto.CreateTradeRequest;
import com.example.tradedesk.dto.TradeResponse;
import com.example.tradedesk.exception.InstrumentNotFoundException;
import com.example.tradedesk.repository.InstrumentRepository;
import com.example.tradedesk.repository.TradeRepository;
import com.example.tradedesk.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class TradeService {

    private final TradeRepository tradeRepository;
    private final InstrumentRepository instrumentRepository;
    private final UserRepository userRepository;

    public TradeService(TradeRepository tradeRepository, InstrumentRepository instrumentRepository, UserRepository userRepository){
        this.tradeRepository = tradeRepository;
        this.instrumentRepository = instrumentRepository;
        this.userRepository = userRepository;
    }

    public TradeResponse createTrade(CreateTradeRequest request) {
        Instrument instrument = instrumentRepository.findById(request.instrumentId())
                .orElseThrow(() -> new InstrumentNotFoundException(request.instrumentId()));

        User testUser = userRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Test user not found — insert one first"));

        Trade trade = new Trade();
        trade.setInstrument(instrument);
        trade.setUser(testUser);
        trade.setSide(request.side());
        trade.setNotional(request.notional());
        trade.setRate(request.rate());
        trade.setTradeDate(request.tradeDate());
        trade.setMaturityDate(request.maturityDate());
        trade.setStatus("OPEN");

        Trade saved = tradeRepository.save(trade);

        return new TradeResponse(
                saved.getId(),
                saved.getInstrument().getTicker(),
                saved.getSide(),
                saved.getNotional(),
                saved.getRate(),
                saved.getTradeDate(),
                saved.getMaturityDate(),
                saved.getStatus()
        );

    }
}