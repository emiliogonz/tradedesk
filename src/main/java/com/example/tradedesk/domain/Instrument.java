package com.example.tradedesk.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
@Entity
@Table(name = "instruments")
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "ticker", nullable = false, unique = true)
    private String ticker;


    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private InstrumentType type;

    @Column(name = "currency_pair", nullable = false)
    private String currencyPair;


    public enum InstrumentType {
        FX_SWAP, DERIVATIVE, FX_SPOT
    }
}