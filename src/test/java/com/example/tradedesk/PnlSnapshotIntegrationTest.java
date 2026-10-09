package com.example.tradedesk;

import com.example.tradedesk.domain.MarketRate;
import com.example.tradedesk.domain.Role;
import com.example.tradedesk.domain.TradeSide;
import com.example.tradedesk.dto.InstrumentResponse;
import com.example.tradedesk.dto.PnlSnapshotResponse;
import com.example.tradedesk.repository.MarketRateRepository;
import com.example.tradedesk.service.PnlSnapshotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PnlSnapshotIntegrationTest extends IntegrationTestBase {

    @Autowired private PnlSnapshotService snapshotService;
    @Autowired private MarketRateRepository marketRateRepository;

    @Test
    void snapshotCapturesRealizedAndUnrealizedPnl() {
        LocalDate date = LocalDate.of(2031, 1, 6);
        InstrumentResponse instrument = createTradableInstrument("SNAP");
        String trader = createUser(Role.TRADER).getEmail();
        tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "1000000", "1.0800"), trader);
        tradeService.createTrade(trade(instrument.id(), TradeSide.SELL, "400000", "1.0900"), trader);
        setMarkDirectly(instrument.id(), "1.1000");

        snapshotService.takeSnapshot(date);

        PnlSnapshotResponse row = snapshotFor(date, instrument.id());
        assertThat(row.currency()).isEqualTo("USD");
        assertThat(row.netExposure()).isEqualByComparingTo("600000");
        assertThat(row.realizedPnl()).isEqualByComparingTo("4000");      // 400k x 0.0100
        assertThat(row.unrealizedPnl()).isEqualByComparingTo("12000");   // 600k x (1.1000 - 1.0800)
    }

    @Test
    void rerunningSnapshotForSameDateReplacesRows() {
        LocalDate date = LocalDate.of(2031, 1, 7);
        InstrumentResponse instrument = createTradableInstrument("RERUN");
        String trader = createUser(Role.TRADER).getEmail();
        tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "1000000", "1.0800"), trader);
        snapshotService.takeSnapshot(date);

        tradeService.createTrade(trade(instrument.id(), TradeSide.SELL, "500000", "1.0900"), trader);
        snapshotService.takeSnapshot(date);

        List<PnlSnapshotResponse> rows = snapshotService.list(date, date).stream()
                .filter(s -> s.instrumentId().equals(instrument.id()))
                .toList();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).netExposure()).isEqualByComparingTo("500000");
        assertThat(rows.get(0).realizedPnl()).isEqualByComparingTo("5000");
    }

    @Test
    void neverTradedInstrumentsAreSkipped() {
        LocalDate date = LocalDate.of(2031, 1, 8);
        InstrumentResponse instrument = createTradableInstrument("UNTRADED");

        snapshotService.takeSnapshot(date);

        assertThat(snapshotService.list(date, date))
                .noneMatch(s -> s.instrumentId().equals(instrument.id()));
    }

    // --- helpers specific to this class ---

    private void setMarkDirectly(Long instrumentId, String rate) {
        MarketRate mark = new MarketRate();
        mark.setInstrumentId(instrumentId);
        mark.setRate(new BigDecimal(rate));
        mark.setMarkedAt(OffsetDateTime.now());
        mark.setMarkedBy("test");
        marketRateRepository.save(mark);
    }

    private PnlSnapshotResponse snapshotFor(LocalDate date, Long instrumentId) {
        return snapshotService.list(date, date).stream()
                .filter(s -> s.instrumentId().equals(instrumentId))
                .findFirst()
                .orElseThrow();
    }
}