package com.example.tradedesk.domain;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@Entity
@Table(name = "risk_limits")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode


public class RiskLimit {

    @Id
    @Column(name="instrument_id")
    private Long instrumentId;

    @Column(name="max_exposure", nullable = false)
    private BigDecimal maxExposure;
}
