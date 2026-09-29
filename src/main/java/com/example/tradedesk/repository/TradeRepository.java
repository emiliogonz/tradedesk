package com.example.tradedesk.repository;

import com.example.tradedesk.domain.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

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
}