package com.example.tradedesk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "pnl_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class PnlSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "net_exposure", nullable = false)
    private BigDecimal netExposure;

    @Column(name = "avg_rate")
    private BigDecimal avgRate;

    @Column(name = "mark_rate")
    private BigDecimal markRate;

    @Column(name = "realized_pnl", nullable = false)
    private BigDecimal realizedPnl;

    @Column(name = "unrealized_pnl")
    private BigDecimal unrealizedPnl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PnlSnapshot other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}