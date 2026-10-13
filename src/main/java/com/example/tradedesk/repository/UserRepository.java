package com.example.tradedesk.repository;

import com.example.tradedesk.domain.Role;
import com.example.tradedesk.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.role = :role ORDER BY u.id")
    List<User> findByRoleForUpdate(@Param("role") Role role);
    List<User> findAllByOrderByIdAsc();
}