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
        SELECT new com.example.tradedesk.dto.PositionResponse(
            p.instrumentId, i.ticker, p.netExposure, p.lastUpdated)
        FROM Position p
        JOIN Instrument i ON i.id = p.instrumentId
        ORDER BY i.ticker
        """)
    List<PositionResponse> findAllWithTicker();
}
