package com.example.tradedesk.service;

import com.example.tradedesk.domain.*;
import com.example.tradedesk.dto.AmendTradeRequest;
import com.example.tradedesk.dto.CreateTradeRequest;
import com.example.tradedesk.dto.TradeResponse;
import com.example.tradedesk.exception.*;
import com.example.tradedesk.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

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

        PnlCalculator.Result result =
                PnlCalculator.apply(current, position.getAvgRate(), delta, request.rate());
        BigDecimal proposed = result.netExposure();

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

        position.setNetExposure(result.netExposure());
        position.setAvgRate(result.avgRate());
        position.setRealizedPnl(position.getRealizedPnl().add(result.realizedPnl()));
        position.setLastUpdated(OffsetDateTime.now());

        return toResponse(saved);
    }
    @Transactional(readOnly = true)
    public Page<TradeResponse> searchTrades(Long instrumentId, LocalDate from, LocalDate to, Pageable pageable)        {
        return tradeRepository.search(instrumentId, from, to, pageable).map(this::toResponse);
    }
    @Transactional(readOnly = true)
    public TradeResponse getTrade(Long tradeId) {
        return tradeRepository.findById(tradeId)
                .map(this::toResponse)
                .orElseThrow(() -> new TradeNotFoundException(tradeId));
    }

    @Transactional
    public TradeResponse cancelTrade(Long tradeId, String actorEmail, boolean actorIsRisk) {
        Correction correction = lockForCorrection(tradeId, actorEmail, actorIsRisk);
        Trade trade = correction.trade();

        trade.setStatus(TradeStatus.CANCELLED);
        trade.setCorrectedAt(OffsetDateTime.now());
        trade.setCorrectedBy(actorEmail);

        rebuildPosition(correction.position());
        return toResponse(trade);
    }

    @Transactional
    public TradeResponse amendTrade(Long tradeId, AmendTradeRequest request, String actorEmail, boolean actorIsRisk) {
        Correction correction = lockForCorrection(tradeId, actorEmail, actorIsRisk);
        Trade original = correction.trade();
        Position position = correction.position();
        BigDecimal exposureBefore = position.getNetExposure();

        original.setStatus(TradeStatus.AMENDED);
        original.setCorrectedAt(OffsetDateTime.now());
        original.setCorrectedBy(actorEmail);

        Trade replacement = new Trade();
        replacement.setInstrument(original.getInstrument());
        replacement.setUser(original.getUser());   // the trade still belongs to the booker
        replacement.setSide(request.side());
        replacement.setNotional(request.notional());
        replacement.setRate(request.rate());
        replacement.setTradeDate(request.tradeDate());
        replacement.setMaturityDate(request.maturityDate());
        replacement.setStatus(TradeStatus.OPEN);
        replacement.setAmendsTradeId(original.getId());
        replacement.setOriginTradeId(
                original.getOriginTradeId() != null ? original.getOriginTradeId() : original.getId());
        Trade saved = tradeRepository.save(replacement);

        rebuildPosition(position);
        enforceLimitIfRiskIncreased(original.getInstrument(), exposureBefore, position.getNetExposure());
        return toResponse(saved);
    }

    private record Correction(Trade trade, Position position) {}

    /** Locks the position, then the trade (always in that order), and checks the actor may correct it. */
    private Correction lockForCorrection(Long tradeId, String actorEmail, boolean actorIsRisk) {
        Long instrumentId = tradeRepository.findInstrumentIdById(tradeId)
                .orElseThrow(() -> new TradeNotFoundException(tradeId));
        Position position = positionRepository.findByIdForUpdate(instrumentId)
                .orElseThrow(() -> new IllegalStateException("No position row for instrument " + instrumentId));
        Trade trade = tradeRepository.findByIdForUpdate(tradeId)
                .orElseThrow(() -> new TradeNotFoundException(tradeId));

        boolean isBooker = trade.getUser().getEmail().equals(actorEmail);
        if (!isBooker && !actorIsRisk) {
            throw new AccessDeniedException("Only the booking trader or RISK can correct this trade");
        }
        if (trade.getStatus() != TradeStatus.OPEN) {
            throw new TradeNotLiveException(tradeId, trade.getStatus());
        }
        return new Correction(trade, position);
    }

    /** Rebuilds the position from every live trade, in booking order, through the same P&L rules as booking. */
    private void rebuildPosition(Position position) {
        List<PnlCalculator.Fill> fills = tradeRepository
                .findByInstrumentAndStatusInBookingOrder(position.getInstrumentId(), TradeStatus.OPEN)
                .stream()
                .map(t -> new PnlCalculator.Fill(signedQty(t.getSide(), t.getNotional()), t.getRate()))
                .toList();

        PnlCalculator.Result rebuilt = PnlCalculator.replay(fills);
        position.setNetExposure(rebuilt.netExposure());
        position.setAvgRate(rebuilt.avgRate());
        position.setRealizedPnl(rebuilt.realizedPnl());
        position.setLastUpdated(OffsetDateTime.now());
    }

    /** An amendment that adds risk faces the same limit as a new trade; one that reduces risk never does. */
    private void enforceLimitIfRiskIncreased(Instrument instrument, BigDecimal before, BigDecimal after) {
        if (after.abs().compareTo(before.abs()) <= 0) {
            return;
        }
        RiskLimit limit = riskLimitRepository.findById(instrument.getId())
                .orElseThrow(() -> new NoRiskLimitException(instrument.getTicker()));
        if (after.abs().compareTo(limit.getMaxExposure()) > 0) {
            throw new RiskLimitExceededException(instrument.getTicker(), after, limit.getMaxExposure());
        }
    }

    private static BigDecimal signedQty(TradeSide side, BigDecimal notional) {
        return side == TradeSide.BUY ? notional : notional.negate();
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
                trade.getStatus(),
                trade.getAmendsTradeId()
        );
    }
}