package com.example.tradedesk.repository;

import com.example.tradedesk.domain.Trade;
import com.example.tradedesk.domain.TradeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    @Query("""
            SELECT t FROM Trade t
            JOIN FETCH t.instrument i
            WHERE (:instrumentId IS NULL OR i.id = :instrumentId)
            AND (:from IS NULL OR t.tradeDate >= :from)
            AND (:to IS NULL OR t.tradeDate <= :to)             
            """)
    Page<Trade> search(
            @Param("instrumentId") Long instrumentId,
            @Param("from")LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable
            );
    @Query("SELECT t.instrument.id FROM Trade t WHERE t.id = :id")
    Optional<Long> findInstrumentIdById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trade t JOIN FETCH t.instrument JOIN FETCH t.user WHERE t.id = :id")
    Optional<Trade> findByIdForUpdate(@Param("id") Long id);

    @Query("""
        SELECT t FROM Trade t
        WHERE t.instrument.id = :instrumentId AND t.status = :status
        ORDER BY COALESCE(t.originTradeId, t.id), t.id
        """)
    List<Trade> findByInstrumentAndStatusInBookingOrder(
            @Param("instrumentId") Long instrumentId,
            @Param("status") TradeStatus status);
}