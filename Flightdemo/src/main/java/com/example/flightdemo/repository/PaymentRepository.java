package com.example.flightdemo.repository;

import com.example.flightdemo.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findBySagaId(String sagaId);
}
