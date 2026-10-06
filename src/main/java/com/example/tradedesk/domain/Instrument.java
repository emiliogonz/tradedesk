package com.example.tradedesk.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
@Entity
@Table(name = "instruments")
@Getter
@Setter
@NoArgsConstructor
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticker", nullable = false, unique = true)
    private String ticker;


    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private InstrumentType type;

    @Column(name = "currency_pair", nullable = false)
    private String currencyPair;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public enum InstrumentType {
        FX_SWAP, DERIVATIVE, FX_SPOT
    }
}