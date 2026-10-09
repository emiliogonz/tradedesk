package com.example.tradedesk.repository;

import com.example.tradedesk.domain.PnlSnapshot;
import com.example.tradedesk.dto.PnlSnapshotResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PnlSnapshotRepository extends JpaRepository<PnlSnapshot, Long> {

    Optional<PnlSnapshot> findBySnapshotDateAndInstrumentId(LocalDate snapshotDate, Long instrumentId);

    @Query("""
            SELECT new com.example.tradedesk.dto.PnlSnapshotResponse(
                s.snapshotDate, s.instrumentId, i.ticker, s.currency,
                s.netExposure, s.markRate, s.realizedPnl, s.unrealizedPnl)
            FROM PnlSnapshot s
            JOIN Instrument i ON i.id = s.instrumentId
            WHERE s.snapshotDate BETWEEN :from AND :to
            ORDER BY s.snapshotDate, i.ticker
            """)
    List<PnlSnapshotResponse> findRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
