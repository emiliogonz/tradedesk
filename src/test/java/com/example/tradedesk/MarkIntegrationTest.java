package com.example.tradedesk;

import com.example.tradedesk.domain.Role;
import com.example.tradedesk.domain.TradeSide;
import com.example.tradedesk.dto.InstrumentResponse;
import com.example.tradedesk.dto.PositionResponse;
import com.example.tradedesk.service.PositionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MarkIntegrationTest extends IntegrationTestBase {

    @Autowired private PositionService positionService;

    @Test
    void riskMarkProducesUnrealizedPnlInQuoteCurrency() throws Exception {
        InstrumentResponse instrument = createTradableInstrument("MARK");
        String trader = createUser(Role.TRADER).getEmail();
        String riskToken = loginAs(createUser(Role.RISK));
        tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "1000000", "1.0800"), trader);

        setMark(riskToken, instrument.id(), "1.0900")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value(instrument.ticker()));

        PositionResponse position = positionOf(instrument.id());
        assertThat(position.pnlCurrency()).isEqualTo("USD");
        assertThat(position.markRate()).isEqualByComparingTo("1.0900");
        assertThat(position.unrealizedPnl()).isEqualByComparingTo("10000");
        assertThat(position.realizedPnl()).isEqualByComparingTo("0");
    }

    @Test
    void positionWithoutMarkHasUnknownUnrealizedPnl() {
        InstrumentResponse instrument = createTradableInstrument("NOMARK");
        String trader = createUser(Role.TRADER).getEmail();
        tradeService.createTrade(trade(instrument.id(), TradeSide.BUY, "500000", "1.0800"), trader);

        PositionResponse position = positionOf(instrument.id());
        assertThat(position.markRate()).isNull();
        assertThat(position.unrealizedPnl()).isNull();
    }

    @Test
    void traderCannotSetMarks() throws Exception {
        InstrumentResponse instrument = createInstrument("MARK-DENIED");
        String traderToken = loginAs(createUser(Role.TRADER));

        setMark(traderToken, instrument.id(), "1.0900")
                .andExpect(status().isForbidden());
    }

    @Test
    void nonPositiveMarkIsRejected() throws Exception {
        InstrumentResponse instrument = createInstrument("MARK-ZERO");
        String riskToken = loginAs(createUser(Role.RISK));

        setMark(riskToken, instrument.id(), "0")
                .andExpect(status().isBadRequest());
    }

    // --- helpers specific to this class ---

    private ResultActions setMark(String token, Long instrumentId, String rate) throws Exception {
        return mockMvc.perform(put("/api/marks/{id}", instrumentId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rate\":" + rate + "}"));
    }

    private PositionResponse positionOf(Long instrumentId) {
        return positionService.list().stream()
                .filter(p -> p.instrumentId().equals(instrumentId))
                .findFirst()
                .orElseThrow();
    }
}