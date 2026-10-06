package com.example.tradedesk.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@Entity
@Table(name = "risk_limits")
@Getter
@Setter
@NoArgsConstructor
public class RiskLimit {

    @Id
    @Column(name="instrument_id")
    private Long instrumentId;

    @Column(name="max_exposure", nullable = false)
    private BigDecimal maxExposure;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RiskLimit other)) return false;
        return instrumentId != null && instrumentId.equals(other.getInstrumentId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
