package com.example.tradedesk.repository;

import com.example.tradedesk.domain.RiskLimit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskLimitRepository extends JpaRepository<RiskLimit, Long> {
}