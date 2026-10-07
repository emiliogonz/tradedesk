package com.example.tradedesk;

import com.example.tradedesk.domain.Role;
import com.example.tradedesk.domain.TradeSide;
import com.example.tradedesk.dto.InstrumentResponse;
import com.example.tradedesk.dto.PositionResponse;
import com.example.tradedesk.repository.TradeRepository;
import com.example.tradedesk.service.PositionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class PositionIntegrationTest extends IntegrationTestBase {

    @Autowired private TradeRepository tradeRepository;
    @Autowired private PositionService positionService;

    @Test
    void newInstrumentStartsWithZeroPosition() {
        InstrumentResponse instrument = createInstrument("ZERO");

        assertThat(exposureOf(instrument.id())).isEqualByComparingTo("0");
    }

    @Test
    void buyIncreasesAndSellDecreasesExposure() {
        InstrumentResponse instrument = createTradableInstrument("SIGN");
        String email = createUser(Role.TRADER).getEmail();

        tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "1000000"), email);
        tradeService.createTrade(trade(instrument.id(), TradeSide.SELL, "400000"), email);

        assertThat(exposureOf(instrument.id())).isEqualByComparingTo("600000");
    }

    @Test
    void concurrentTradesOnSameInstrumentDoNotLoseUpdates() throws Exception {
        InstrumentResponse instrument = createTradableInstrument("CONC");
        String email = createUser(Role.TRADER).getEmail();

        int threads = 20;
        BigDecimal notionalPerTrade = new BigDecimal("1000");

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                startGate.await();
                tradeService.createTrade(
                        trade(instrument.id(), TradeSide.BUY, notionalPerTrade.toPlainString()), email);
                return null;
            }));
        }
        startGate.countDown();

        for (Future<?> result : results) {
            result.get();
        }
        pool.shutdown();

        BigDecimal expected = notionalPerTrade.multiply(BigDecimal.valueOf(threads));
        assertThat(exposureOf(instrument.id())).isEqualByComparingTo(expected);
        assertThat(tradeRepository.count()).isGreaterThanOrEqualTo(threads);
    }

    @Test
    void listReturnsPositionWithTicker() {
        InstrumentResponse instrument = createTradableInstrument("LIST");
        String email = createUser(Role.TRADER).getEmail();
        tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "250000"), email);

        PositionResponse position = positionService.list().stream()
                .filter(p -> p.instrumentId().equals(instrument.id()))
                .findFirst()
                .orElseThrow();

        assertThat(position.ticker()).isEqualTo(instrument.ticker());
        assertThat(position.netExposure()).isEqualByComparingTo("250000");
    }
}