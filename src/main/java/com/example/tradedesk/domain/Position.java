package com.example.tradedesk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "positions")
@Getter
@Setter
@NoArgsConstructor

public class Position {
    @Id
    @Column(name = "instrument_id")
    private Long instrumentId;

    @Column(name = "net_exposure", nullable = false)
    private BigDecimal netExposure;

    @Column(name = "last_updated", nullable = false)
    private OffsetDateTime lastUpdated;
}
