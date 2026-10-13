package com.example.tradedesk;

import com.example.tradedesk.domain.Position;
import com.example.tradedesk.domain.Role;
import com.example.tradedesk.domain.TradeSide;
import com.example.tradedesk.domain.TradeStatus;
import com.example.tradedesk.dto.AmendTradeRequest;
import com.example.tradedesk.dto.InstrumentResponse;
import com.example.tradedesk.dto.TradeResponse;
import com.example.tradedesk.exception.RiskLimitExceededException;
import com.example.tradedesk.exception.TradeNotLiveException;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TradeCorrectionIntegrationTest extends IntegrationTestBase {

    @Test
    void cancellingAnEarlierTradeReplaysLaterTradesAgainstTheCorrectAverage() {
        InstrumentResponse instrument = createTradableInstrument("CXL");
        String trader = createUser(Role.TRADER).getEmail();
        TradeResponse first = tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "1000000", "1.0800"), trader);
        tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "1000000", "1.0900"), trader);
        tradeService.createTrade(trade(instrument.id(), TradeSide.SELL, "1000000", "1.1000"), trader);
        assertThat(position(instrument.id()).getRealizedPnl()).isEqualByComparingTo("15000");

        TradeResponse cancelled = tradeService.cancelTrade(first.id(), trader, false);

        assertThat(cancelled.status()).isEqualTo(TradeStatus.CANCELLED);
        Position position = position(instrument.id());
        assertThat(position.getRealizedPnl()).isEqualByComparingTo("10000");
        assertThat(position.getNetExposure()).isEqualByComparingTo("0");
        assertThat(position.getAvgRate()).isNull();
    }

    @Test
    void amendedTradeKeepsItsPlaceInTheBookingSequence() {
        InstrumentResponse instrument = createTradableInstrument("AMD");
        String trader = createUser(Role.TRADER).getEmail();
        TradeResponse first = tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "1000000", "1.0800"), trader);
        tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "1000000", "1.0900"), trader);
        tradeService.createTrade(trade(instrument.id(), TradeSide.SELL, "1000000", "1.1000"), trader);

        TradeResponse replacement = tradeService.amendTrade(first.id(), amend(TradeSide.BUY, "1000000", "1.0700"), trader, false);

        // Replayed in the original's slot: avg (1.07 + 1.09) / 2 = 1.08, then sell realizes 1M x 0.02.
        // Appended at the end instead, the result would be avg 1.07 with 10,000 realized.
        Position position = position(instrument.id());
        assertThat(position.getRealizedPnl()).isEqualByComparingTo("20000");
        assertThat(position.getAvgRate()).isEqualByComparingTo("1.0800");
        assertThat(position.getNetExposure()).isEqualByComparingTo("1000000");

        assertThat(replacement.amendsTradeId()).isEqualTo(first.id());
        assertThat(tradeService.getTrade(first.id()).status()).isEqualTo(TradeStatus.AMENDED);
    }

    @Test
    void onlyTheBookerOrRiskCanCorrectATrade() {
        InstrumentResponse instrument = createTradableInstrument("PERM");
        String booker = createUser(Role.TRADER).getEmail();
        String otherTrader = createUser(Role.TRADER).getEmail();
        String risk = createUser(Role.RISK).getEmail();
        TradeResponse booked = tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "500000", "1.0850"), booker);

        assertThatThrownBy(() -> tradeService.cancelTrade(booked.id(), otherTrader, false))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(tradeService.getTrade(booked.id()).status()).isEqualTo(TradeStatus.OPEN);

        tradeService.cancelTrade(booked.id(), risk, true);
        assertThat(tradeService.getTrade(booked.id()).status()).isEqualTo(TradeStatus.CANCELLED);
    }

    @Test
    void aCorrectedTradeCannotBeCorrectedAgain() {
        InstrumentResponse instrument = createTradableInstrument("TWICE");
        String trader = createUser(Role.TRADER).getEmail();
        TradeResponse booked = tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "500000", "1.0850"), trader);
        tradeService.cancelTrade(booked.id(), trader, false);

        assertThatThrownBy(() -> tradeService.cancelTrade(booked.id(), trader, false))
                .isInstanceOf(TradeNotLiveException.class);
        assertThatThrownBy(() -> tradeService.amendTrade(booked.id(), amend(TradeSide.BUY, "1", "1.0"), trader, false))
                .isInstanceOf(TradeNotLiveException.class);
    }

    @Test
    void amendmentThatBreachesTheLimitIsRejectedAndNothingChanges() {
        InstrumentResponse instrument = createInstrument("AMD-LIMIT");
        setLimitDirectly(instrument.id(), "1000000");
        String trader = createUser(Role.TRADER).getEmail();
        TradeResponse booked = tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "800000", "1.0850"), trader);

        assertThatThrownBy(() -> tradeService.amendTrade(booked.id(), amend(TradeSide.BUY, "1200000", "1.0850"), trader, false))
                .isInstanceOf(RiskLimitExceededException.class);

        assertThat(tradeService.getTrade(booked.id()).status()).isEqualTo(TradeStatus.OPEN);
        assertThat(position(instrument.id()).getNetExposure()).isEqualByComparingTo("800000");
    }

    // --- helpers specific to this class ---

    private Position position(Long instrumentId) {
        return positionRepository.findById(instrumentId).orElseThrow();
    }

    private static AmendTradeRequest amend(TradeSide side, String notional, String rate) {
        return new AmendTradeRequest(side, new BigDecimal(notional), new BigDecimal(rate), LocalDate.now(), null);
    }
}