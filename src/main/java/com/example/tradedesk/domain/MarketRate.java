package com.example.tradedesk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "market_rates")
@Getter
@Setter
@NoArgsConstructor
public class MarketRate {

    @Id
    @Column(name = "instrument_id")
    private Long instrumentId;

    @Column(nullable = false)
    private BigDecimal rate;

    @Column(name = "marked_at", nullable = false)
    private OffsetDateTime markedAt;

    @Column(name = "marked_by", nullable = false)
    private String markedBy;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MarketRate other)) return false;
        return instrumentId != null && instrumentId.equals(other.getInstrumentId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}