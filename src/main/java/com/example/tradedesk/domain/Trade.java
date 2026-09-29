package com.example.tradedesk.domain;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "trades")
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor

public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
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
}
