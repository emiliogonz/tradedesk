package com.example.tradedesk.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PnlCalculator {
    public static final int RATE_SCALE = 8;
    public static final int PNL_SCALE = 4;

    private PnlCalculator() {}

    public record Result(BigDecimal netExposure, BigDecimal avgRate, BigDecimal realizedPnl) {}

    public static Result apply(BigDecimal exposure, BigDecimal avgRate,
                               BigDecimal signedQty, BigDecimal tradeRate) {

        if (exposure.signum() == 0 || exposure.signum() == signedQty.signum()) {
            BigDecimal newExposure = exposure.add(signedQty);
            BigDecimal newAvg = exposure.signum() == 0
                    ? tradeRate
                    : exposure.multiply(avgRate)
                      .add(signedQty.multiply(tradeRate))
                      .divide(newExposure, RATE_SCALE, RoundingMode.HALF_EVEN);
            return new Result(newExposure, newAvg, BigDecimal.ZERO);
        }

        // Opposite direction: close some or all of the position, possibly flipping through zero.
        BigDecimal closedQty = signedQty.abs().min(exposure.abs());
        BigDecimal realized = closedQty
                .multiply(tradeRate.subtract(avgRate))
                .multiply(BigDecimal.valueOf(exposure.signum()))
                .setScale(PNL_SCALE, RoundingMode.HALF_EVEN);

        BigDecimal newExposure = exposure.add(signedQty);
        BigDecimal newAvg;
        if (newExposure.signum() == 0) {
            newAvg = null;                       // flat: no open position, no average
        } else if (newExposure.signum() == exposure.signum()) {
            newAvg = avgRate;                    // partially reduced: average unchanged
        } else {
            newAvg = tradeRate;                  // flipped: the remainder opened at this trade's rate
        }
        return new Result(newExposure, newAvg, realized);
    }
    public static BigDecimal unrealized(BigDecimal exposure, BigDecimal avgRate, BigDecimal mark) {
        if (exposure.signum() == 0){
            return BigDecimal.ZERO.setScale(PNL_SCALE);
        }
        if (avgRate == null || mark == null) {
            return null;
        }
        return exposure.multiply(mark.subtract(avgRate)).setScale(PNL_SCALE, RoundingMode.HALF_EVEN);
    }
}
