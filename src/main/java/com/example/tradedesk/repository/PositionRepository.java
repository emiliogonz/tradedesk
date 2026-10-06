package com.example.tradedesk.repository;

import com.example.tradedesk.domain.Position;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long>{

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Position p WHERE p.instrumentId = :instrumentId")
    Optional<Position> findByIdForUpdate(@Param("instrumentId") Long instrumentId);
}
