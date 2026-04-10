package com.example.flightdemo.repository;

import com.example.flightdemo.entity.LoyaltyPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoyaltyPointsRepository extends JpaRepository<LoyaltyPoints, Long> {
    Optional<LoyaltyPoints> findBySagaId(String sagaId);
}
