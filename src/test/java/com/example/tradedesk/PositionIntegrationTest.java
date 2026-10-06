package com.example.tradedesk;

import com.example.tradedesk.domain.Instrument.InstrumentType;
import com.example.tradedesk.domain.Position;
import com.example.tradedesk.domain.RiskLimit;
import com.example.tradedesk.domain.TradeSide;
import com.example.tradedesk.domain.User;
import com.example.tradedesk.dto.CreateInstrumentRequest;
import com.example.tradedesk.dto.CreateTradeRequest;
import com.example.tradedesk.dto.InstrumentResponse;
import com.example.tradedesk.dto.PositionResponse;
import com.example.tradedesk.repository.PositionRepository;
import com.example.tradedesk.repository.RiskLimitRepository;
import com.example.tradedesk.repository.TradeRepository;
import com.example.tradedesk.repository.UserRepository;
import com.example.tradedesk.service.InstrumentService;
import com.example.tradedesk.service.PositionService;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class PositionIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private InstrumentService instrumentService;
    @Autowired private TradeService tradeService;
    @Autowired private PositionRepository positionRepository;
    @Autowired private TradeRepository tradeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PositionService positionService;
    @Autowired private RiskLimitRepository riskLimitRepository;

    @Test
    void listReturnsPositionWithTicker() {
        InstrumentResponse instrument = createTradableInstrument("LIST-TEST");
        String email = createUser("list@tradedesk.dev");
        tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "250000"), email);

        PositionResponse position = positionService.list().stream()
                .filter(p -> p.instrumentId().equals(instrument.id()))
                .findFirst()
                .orElseThrow();

        assertThat(position.ticker()).isEqualTo("LIST-TEST");
        assertThat(position.netExposure()).isEqualByComparingTo("250000");
    }
    @Test
    void newInstrumentStartsWithZeroPosition() {
        InstrumentResponse instrument = createTradableInstrument("ZERO-TEST");

        Position position = positionRepository.findById(instrument.id()).orElseThrow();

        assertThat(position.getNetExposure()).isEqualByComparingTo("0");
    }

    @Test
    void buyIncreasesAndSellDecreasesExposure() {
        InstrumentResponse instrument = createTradableInstrument("SIGN-TEST");
        String email = createUser("sign@tradedesk.dev");

        tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "1000000"), email);
        tradeService.createTrade(trade(instrument.id(), TradeSide.SELL, "400000"), email);

        Position position = positionRepository.findById(instrument.id()).orElseThrow();
        assertThat(position.getNetExposure()).isEqualByComparingTo("600000");
    }

    @Test
    void concurrentTradesOnSameInstrumentDoNotLoseUpdates() throws Exception {
        InstrumentResponse instrument = createTradableInstrument("CONC-TEST");
        String email = createUser("concurrent@tradedesk.dev");

        int threads = 20;
        BigDecimal notionalPerTrade = new BigDecimal("1000");

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                startGate.await();   // every thread waits here...
                tradeService.createTrade(
                        trade(instrument.id(), TradeSide.BUY, notionalPerTrade.toPlainString()), email);
                return null;
            }));
        }

        startGate.countDown();       // ...then all are released at once

        for (Future<?> result : results) {
            result.get();            // rethrows any exception from the thread
        }
        pool.shutdown();

        Position position = positionRepository.findById(instrument.id()).orElseThrow();
        BigDecimal expected = notionalPerTrade.multiply(BigDecimal.valueOf(threads));

        assertThat(position.getNetExposure()).isEqualByComparingTo(expected);
        assertThat(tradeRepository.count()).isGreaterThanOrEqualTo(threads);
    }

    // --- helpers ---

    private InstrumentResponse createInstrument(String ticker) {
        return instrumentService.create(
                new CreateInstrumentRequest(ticker, InstrumentType.FX_SPOT, "EUR/USD"));
    }
    private InstrumentResponse createTradableInstrument(String ticker) {
        InstrumentResponse instrument = createInstrument(ticker);
        RiskLimit limit = new RiskLimit();
        limit.setInstrumentId(instrument.id());
        limit.setMaxExposure(new BigDecimal("1000000000"));
        riskLimitRepository.save(limit);
        return instrument;
    }

    private String createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("not-used-in-this-test");
        user.setRole("TRADER");
        userRepository.save(user);
        return email;
    }

    private CreateTradeRequest trade(Long instrumentId, TradeSide side, String notional) {
        return new CreateTradeRequest(
                instrumentId, side, new BigDecimal(notional), new BigDecimal("1.0850"),
                LocalDate.now(), null);
    }
}