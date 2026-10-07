package com.example.tradedesk;

import com.example.tradedesk.domain.Role;
import com.example.tradedesk.domain.TradeSide;
import com.example.tradedesk.dto.InstrumentResponse;
import com.example.tradedesk.exception.NoRiskLimitException;
import com.example.tradedesk.exception.RiskLimitExceededException;
import com.example.tradedesk.repository.TradeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskLimitIntegrationTest extends IntegrationTestBase {

    @Autowired private TradeRepository tradeRepository;

    @Test
    void tradeRejectedWhenInstrumentHasNoLimit() {
        InstrumentResponse instrument = createInstrument("NOLIMIT");
        String email = createUser(Role.TRADER).getEmail();

        assertThatThrownBy(() ->
                tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "1000"), email))
                .isInstanceOf(NoRiskLimitException.class);
    }

    @Test
    void tradeRejectedWhenItWouldBreachLimit_andNothingIsWritten() {
        InstrumentResponse instrument = createInstrument("BREACH");
        setLimitDirectly(instrument.id(), "1000000");
        String email = createUser(Role.TRADER).getEmail();
        long tradesBefore = tradeRepository.count();

        assertThatThrownBy(() ->
                tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "1200000"), email))
                .isInstanceOf(RiskLimitExceededException.class)
                .hasMessageContaining("limit is 1000000");

        assertThat(tradeRepository.count()).isEqualTo(tradesBefore);
        assertThat(exposureOf(instrument.id())).isEqualByComparingTo("0");
    }

    @Test
    void riskReducingTradeAllowedEvenWhenOverLimit() {
        InstrumentResponse instrument = createInstrument("REDUCE");
        setLimitDirectly(instrument.id(), "1000000");
        String email = createUser(Role.TRADER).getEmail();
        tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "900000"), email);

        setLimitDirectly(instrument.id(), "500000");

        tradeService.createTrade(trade(instrument.id(), TradeSide.SELL, "200000"), email);
        assertThat(exposureOf(instrument.id())).isEqualByComparingTo("700000");

        assertThatThrownBy(() ->
                tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "1"), email))
                .isInstanceOf(RiskLimitExceededException.class);
    }

    @Test
    void concurrentTradesCannotJointlyBreachLimit() throws Exception {
        InstrumentResponse instrument = createInstrument("CONC-LIMIT");
        setLimitDirectly(instrument.id(), "1000000");
        String email = createUser(Role.TRADER).getEmail();

        int threads = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                startGate.await();
                tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "300000"), email);
                return null;
            }));
        }
        startGate.countDown();

        int succeeded = 0;
        int rejected = 0;
        for (Future<?> result : results) {
            try {
                result.get();
                succeeded++;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RiskLimitExceededException) {
                    rejected++;
                } else {
                    throw e;
                }
            }
        }
        pool.shutdown();

        assertThat(succeeded).isEqualTo(3);
        assertThat(rejected).isEqualTo(2);
        assertThat(exposureOf(instrument.id())).isEqualByComparingTo("900000");
    }
}