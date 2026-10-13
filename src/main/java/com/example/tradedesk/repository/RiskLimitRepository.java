package com.example.tradedesk.repository;

import com.example.tradedesk.domain.RiskLimit;
import com.example.tradedesk.dto.RiskLimitResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RiskLimitRepository extends JpaRepository<RiskLimit, Long> {
    @Query("""
       SELECT new com.example.tradedesk.dto.RiskLimitResponse(r.instrumentId, i.ticker, r.maxExposure)
       FROM RiskLimit r, Instrument i
       WHERE i.id = r.instrumentId
       ORDER BY i.ticker
       """)
    List<RiskLimitResponse> findAllWithTicker();
}