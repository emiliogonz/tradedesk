package com.example.tradedesk;

import com.example.tradedesk.domain.Instrument.InstrumentType;
import com.example.tradedesk.domain.RiskLimit;
import com.example.tradedesk.domain.Role;
import com.example.tradedesk.domain.TradeSide;
import com.example.tradedesk.domain.User;
import com.example.tradedesk.dto.CreateInstrumentRequest;
import com.example.tradedesk.dto.CreateTradeRequest;
import com.example.tradedesk.dto.InstrumentResponse;
import com.example.tradedesk.repository.PositionRepository;
import com.example.tradedesk.repository.RiskLimitRepository;
import com.example.tradedesk.repository.UserRepository;
import com.example.tradedesk.service.InstrumentService;
import com.example.tradedesk.service.TradeService;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationTestBase {

    // One container for the whole test run. Started once when this class loads;
    // Testcontainers' Ryuk container removes it when the JVM exits.
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    protected static final String PASSWORD = "testpassword123";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PositionRepository positionRepository;
    @Autowired protected RiskLimitRepository riskLimitRepository;
    @Autowired protected InstrumentService instrumentService;
    @Autowired protected TradeService tradeService;

    /** The database is shared across all test classes, so names must never collide. */
    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected User createUser(Role role) {
        User user = new User();
        user.setEmail(unique(role.name().toLowerCase()) + "@tradedesk.dev");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        return userRepository.save(user);
    }

    protected String loginAs(User user) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    protected InstrumentResponse createInstrument(String tickerPrefix) {
        return instrumentService.create(
                new CreateInstrumentRequest(unique(tickerPrefix), InstrumentType.FX_SPOT, "EUR/USD"));
    }

    /** An instrument with a limit so large it never affects the test. */
    protected InstrumentResponse createTradableInstrument(String tickerPrefix) {
        InstrumentResponse instrument = createInstrument(tickerPrefix);
        setLimitDirectly(instrument.id(), "1000000000");
        return instrument;
    }

    /** Writes a limit straight to the repository, bypassing the RISK-only endpoint. */
    protected void setLimitDirectly(Long instrumentId, String maxExposure) {
        RiskLimit limit = new RiskLimit();
        limit.setInstrumentId(instrumentId);
        limit.setMaxExposure(new BigDecimal(maxExposure));
        riskLimitRepository.save(limit);
    }

    protected BigDecimal exposureOf(Long instrumentId) {
        return positionRepository.findById(instrumentId).orElseThrow().getNetExposure();
    }

    protected CreateTradeRequest trade(Long instrumentId, TradeSide side, String notional) {
        return new CreateTradeRequest(
                instrumentId, side, new BigDecimal(notional), new BigDecimal("1.0850"),
                LocalDate.now(), null);
    }
}