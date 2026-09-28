package com.example.tradedesk.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;

@Entity
@Table(name = "risk_limits")
@Getter
@Setter


public class RiskLimits {

    @Id
    @Column(name="instrument_id")
    private Long instrumentId;

    @Column(name="max_exposure", nullable = false)
    private BigDecimal maxExposure;
}
