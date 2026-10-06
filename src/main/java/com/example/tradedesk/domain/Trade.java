package com.example.tradedesk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Enumerated(EnumType.STRING)
    @Column(name="side", nullable = false)
    private TradeSide side;

    @Column(name="notional", nullable = false)
    private BigDecimal notional;

    @Column(name="rate", nullable = false)
    private BigDecimal rate;

    @Column(name="trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name="maturity_date")
    private LocalDate maturityDate;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false)
    private TradeStatus status;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trade other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
