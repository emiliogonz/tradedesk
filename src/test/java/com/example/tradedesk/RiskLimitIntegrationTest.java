package com.example.tradedesk;

import com.example.tradedesk.domain.*;
import com.example.tradedesk.domain.Instrument.InstrumentType;
import com.example.tradedesk.dto.CreateInstrumentRequest;
import com.example.tradedesk.dto.CreateTradeRequest;
import com.example.tradedesk.dto.InstrumentResponse;
import com.example.tradedesk.exception.NoRiskLimitException;
import com.example.tradedesk.exception.RiskLimitExceededException;
import com.example.tradedesk.repository.PositionRepository;
import com.example.tradedesk.repository.RiskLimitRepository;
import com.example.tradedesk.repository.TradeRepository;
import com.example.tradedesk.repository.UserRepository;
import com.example.tradedesk.service.InstrumentService;
import com.example.tradedesk.service.TradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
class RiskLimitIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private InstrumentService instrumentService;
    @Autowired private TradeService tradeService;
    @Autowired private PositionRepository positionRepository;
    @Autowired private RiskLimitRepository riskLimitRepository;
    @Autowired private TradeRepository tradeRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void tradeRejectedWhenInstrumentHasNoLimit() {
        InstrumentResponse instrument = createInstrument("NOLIMIT-TEST");
        String email = createUser("nolimit@tradedesk.dev");

        assertThatThrownBy(() ->
                tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "1000"), email))
                .isInstanceOf(NoRiskLimitException.class);
    }

    @Test
    void tradeRejectedWhenItWouldBreachLimit_andNothingIsWritten() {
        InstrumentResponse instrument = createInstrument("BREACH-TEST");
        setLimit(instrument.id(), "1000000");
        String email = createUser("breach@tradedesk.dev");
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
        InstrumentResponse instrument = createInstrument("REDUCE-TEST");
        setLimit(instrument.id(), "1000000");
        String email = createUser("reduce@tradedesk.dev");
        tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "900000"), email);

        setLimit(instrument.id(), "500000");   // risk lowers the limit; desk is now over it

        // Moving toward zero is allowed...
        tradeService.createTrade(trade(instrument.id(), TradeSide.SELL, "200000"), email);
        assertThat(exposureOf(instrument.id())).isEqualByComparingTo("700000");

        // ...but adding to the position is not.
        assertThatThrownBy(() ->
                tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "1"), email))
                .isInstanceOf(RiskLimitExceededException.class);
    }

    @Test
    void concurrentTradesCannotJointlyBreachLimit() throws Exception {
        InstrumentResponse instrument = createInstrument("CONC-LIMIT-TEST");
        setLimit(instrument.id(), "1000000");
        String email = createUser("conclimit@tradedesk.dev");

        int threads = 5;   // 5 x 300k = 1.5M requested against a 1M limit
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

    // --- helpers ---

    private InstrumentResponse createInstrument(String ticker) {
        return instrumentService.create(
                new CreateInstrumentRequest(ticker, InstrumentType.FX_SPOT, "EUR/USD"));
    }

    private void setLimit(Long instrumentId, String maxExposure) {
        RiskLimit limit = new RiskLimit();
        limit.setInstrumentId(instrumentId);
        limit.setMaxExposure(new BigDecimal(maxExposure));
        riskLimitRepository.save(limit);
    }

    private BigDecimal exposureOf(Long instrumentId) {
        Position position = positionRepository.findById(instrumentId).orElseThrow();
        return position.getNetExposure();
    }

    private String createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("not-used-in-this-test");
        user.setRole(Role.TRADER);
        userRepository.save(user);
        return email;
    }

    private CreateTradeRequest trade(Long instrumentId, TradeSide side, String notional) {
        return new CreateTradeRequest(
                instrumentId, side, new BigDecimal(notional), new BigDecimal("1.0850"),
                LocalDate.now(), null);
    }
}