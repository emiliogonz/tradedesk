package com.example.tradedesk;

import com.example.tradedesk.domain.Position;
import com.example.tradedesk.domain.Role;
import com.example.tradedesk.domain.TradeSide;
import com.example.tradedesk.dto.InstrumentResponse;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RiskLimitApiIntegrationTest extends IntegrationTestBase {

    @Test
    void riskUserSetsLimit_thenTraderCanTrade() throws Exception {
        InstrumentResponse instrument = createInstrument("E2E");
        String riskToken = loginAs(createUser(Role.RISK));
        String traderToken = loginAs(createUser(Role.TRADER));

        bookTrade(traderToken, instrument.id(), "500000")
                .andExpect(status().isUnprocessableEntity());

        setLimit(riskToken, instrument.id(), "1000000")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value(instrument.ticker()));

        bookTrade(traderToken, instrument.id(), "500000")
                .andExpect(status().isCreated());
    }

    @Test
    void settingLimitAgainReplacesIt() throws Exception {
        InstrumentResponse instrument = createInstrument("REPLACE");
        String riskToken = loginAs(createUser(Role.RISK));

        setLimit(riskToken, instrument.id(), "1000000").andExpect(status().isOk());
        setLimit(riskToken, instrument.id(), "2500000").andExpect(status().isOk());

        assertThat(riskLimitRepository.findById(instrument.id()).orElseThrow().getMaxExposure())
                .isEqualByComparingTo("2500000");
    }

    @Test
    void traderCannotSetLimits() throws Exception {
        InstrumentResponse instrument = createInstrument("TRADER-DENIED");
        String traderToken = loginAs(createUser(Role.TRADER));

        setLimit(traderToken, instrument.id(), "1000000")
                .andExpect(status().isForbidden());

        assertThat(riskLimitRepository.findById(instrument.id())).isEmpty();
    }

    @Test
    void adminCannotSetLimits_segregationOfDuties() throws Exception {
        InstrumentResponse instrument = createInstrument("ADMIN-DENIED");
        String adminToken = loginAs(createUser(Role.ADMIN));

        setLimit(adminToken, instrument.id(), "1000000")
                .andExpect(status().isForbidden());

        assertThat(riskLimitRepository.findById(instrument.id())).isEmpty();
    }

    @Test
    void negativeLimitIsRejected() throws Exception {
        InstrumentResponse instrument = createInstrument("NEGATIVE");
        String riskToken = loginAs(createUser(Role.RISK));

        setLimit(riskToken, instrument.id(), "-1")
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownInstrumentReturns404() throws Exception {
        String riskToken = loginAs(createUser(Role.RISK));

        setLimit(riskToken, 999_999L, "1000000")
                .andExpect(status().isNotFound());
    }

    @Test
    void tradesMaintainAverageRateAndRealizedPnl() {
        InstrumentResponse instrument = createTradableInstrument("PNL");
        String email = createUser(Role.TRADER).getEmail();

        tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "1000000", "1.0800"), email);
        tradeService.createTrade(trade(instrument.id(), TradeSide.SELL, "400000", "1.0900"), email);

        Position position = positionRepository.findById(instrument.id()).orElseThrow();
        assertThat(position.getNetExposure()).isEqualByComparingTo("600000");
        assertThat(position.getAvgRate()).isEqualByComparingTo("1.0800");
        assertThat(position.getRealizedPnl()).isEqualByComparingTo("4000");
    }

    @Test
    void traderCanReadLimits() throws Exception {
        InstrumentResponse instrument = createInstrument("RL");
        setLimitDirectly(instrument.id(), "1000000");
        mockMvc.perform(get("/api/risk-limits").header("Authorization", "Bearer " + loginAs(createUser(Role.TRADER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.instrumentId == " + instrument.id() + ")].ticker").value(instrument.ticker()));
    }

    // --- helpers specific to this class ---

    private ResultActions setLimit(String token, Long instrumentId, String maxExposure) throws Exception {
        return mockMvc.perform(put("/api/risk-limits/{id}", instrumentId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxExposure\":" + maxExposure + "}"));
    }

    private ResultActions bookTrade(String token, Long instrumentId, String notional) throws Exception {
        return mockMvc.perform(post("/api/trades")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"instrumentId": %d, "side": "BUY", "notional": %s,
                         "rate": 1.0850, "tradeDate": "%s"}
                        """.formatted(instrumentId, notional, LocalDate.now())));
    }
}