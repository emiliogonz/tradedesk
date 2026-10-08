package com.example.tradedesk.repository;

import com.example.tradedesk.domain.MarketRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketRateRepository extends JpaRepository<MarketRate, Long> {
}