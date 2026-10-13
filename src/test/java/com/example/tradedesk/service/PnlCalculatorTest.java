package com.example.tradedesk.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PnlCalculatorTest {

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    @Test
    void openingFromFlatTakesTradeRate() {
        var r = PnlCalculator.apply(bd("0"), null, bd("1000000"), bd("1.0800"));

        assertThat(r.netExposure()).isEqualByComparingTo("1000000");
        assertThat(r.avgRate()).isEqualByComparingTo("1.0800");
        assertThat(r.realizedPnl()).isEqualByComparingTo("0");
    }

    @Test
    void addingToLongBlendsAverage() {
        var r = PnlCalculator.apply(bd("1000000"), bd("1.0800"), bd("1000000"), bd("1.0900"));

        assertThat(r.netExposure()).isEqualByComparingTo("2000000");
        assertThat(r.avgRate()).isEqualByComparingTo("1.0850");
        assertThat(r.realizedPnl()).isEqualByComparingTo("0");
    }

    @Test
    void partialReduceRealizesAndKeepsAverage() {
        var r = PnlCalculator.apply(bd("1500000"), bd("1.0850"), bd("-500000"), bd("1.0950"));

        assertThat(r.netExposure()).isEqualByComparingTo("1000000");
        assertThat(r.avgRate()).isEqualByComparingTo("1.0850");
        assertThat(r.realizedPnl()).isEqualByComparingTo("5000");   // 500k x 0.0100
    }

    @Test
    void fullCloseRealizesAndClearsAverage() {
        var r = PnlCalculator.apply(bd("1000000"), bd("1.0800"), bd("-1000000"), bd("1.0750"));

        assertThat(r.netExposure()).isEqualByComparingTo("0");
        assertThat(r.avgRate()).isNull();
        assertThat(r.realizedPnl()).isEqualByComparingTo("-5000");  // sold below cost: a loss
    }

    @Test
    void flipFromLongToShortRealizesOnlyTheClosedPart() {
        var r = PnlCalculator.apply(bd("1000000"), bd("1.0800"), bd("-1500000"), bd("1.0900"));

        assertThat(r.netExposure()).isEqualByComparingTo("-500000");
        assertThat(r.avgRate()).isEqualByComparingTo("1.0900");     // new short opened at trade rate
        assertThat(r.realizedPnl()).isEqualByComparingTo("10000");  // only the 1M closed
    }

    @Test
    void shortCoveredBelowAverageIsAGain() {
        var r = PnlCalculator.apply(bd("-1000000"), bd("1.0900"), bd("400000"), bd("1.0800"));

        assertThat(r.netExposure()).isEqualByComparingTo("-600000");
        assertThat(r.avgRate()).isEqualByComparingTo("1.0900");
        assertThat(r.realizedPnl()).isEqualByComparingTo("4000");   // 400k x 0.0100, short side
    }

    @Test
    void unrealizedOnLongWhenMarkRises() {
        assertThat(PnlCalculator.unrealized(bd("1000000"), bd("1.0800"), bd("1.0900")))
                .isEqualByComparingTo("10000");
    }

    @Test
    void unrealizedOnShortWhenMarkFalls() {
        assertThat(PnlCalculator.unrealized(bd("-1000000"), bd("1.0900"), bd("1.0800")))
                .isEqualByComparingTo("10000");
    }

    @Test
    void unrealizedIsUnknownWithoutMark() {
        assertThat(PnlCalculator.unrealized(bd("1000000"), bd("1.0800"), null)).isNull();
    }

    @Test
    void unrealizedIsZeroWhenFlat() {
        assertThat(PnlCalculator.unrealized(bd("0"), null, null)).isEqualByComparingTo("0");
    }
    @Test
    void replayReflectsACancelledEarlierTrade() {
        var all = PnlCalculator.replay(List.of(
                fill("1000000", "1.0800"), fill("1000000", "1.0900"), fill("-1000000", "1.1000")));
        assertThat(all.realizedPnl()).isEqualByComparingTo("15000");   // sold against avg 1.0850
        assertThat(all.netExposure()).isEqualByComparingTo("1000000");

        var withoutFirst = PnlCalculator.replay(List.of(
                fill("1000000", "1.0900"), fill("-1000000", "1.1000")));
        assertThat(withoutFirst.realizedPnl()).isEqualByComparingTo("10000");   // sold against avg 1.0900
        assertThat(withoutFirst.netExposure()).isEqualByComparingTo("0");
        assertThat(withoutFirst.avgRate()).isNull();
    }

    private static PnlCalculator.Fill fill(String qty, String rate) {
        return new PnlCalculator.Fill(bd(qty), bd(rate));
    }
}