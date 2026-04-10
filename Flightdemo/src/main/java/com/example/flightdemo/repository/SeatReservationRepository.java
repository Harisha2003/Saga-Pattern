package com.example.flightdemo.repository;

import com.example.flightdemo.entity.SeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {
    Optional<SeatReservation> findBySagaId(String sagaId);
}
