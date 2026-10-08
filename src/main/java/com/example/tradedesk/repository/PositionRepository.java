package com.example.tradedesk.repository;

import com.example.tradedesk.domain.Position;
import com.example.tradedesk.dto.PositionResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long>{

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Position p WHERE p.instrumentId = :instrumentId")
    Optional<Position> findByIdForUpdate(@Param("instrumentId") Long instrumentId);
    @Query("""
        SELECT new com.example.tradedesk.repository.PositionRow(
            p.instrumentId, i.ticker, i.currencyPair,
            p.netExposure, p.avgRate, p.realizedPnl,
            m.rate, r.maxExposure, p.lastUpdated)
        FROM Position p
        JOIN Instrument i ON i.id = p.instrumentId
        LEFT JOIN MarketRate m ON m.instrumentId = p.instrumentId
        LEFT JOIN RiskLimit r ON r.instrumentId = p.instrumentId
        ORDER BY i.ticker
        """)
    List<PositionRow> findAllRows();
}
