package com.example.flightdemo.service;

import com.example.flightdemo.entity.SeatReservation;
import com.example.flightdemo.exception.SagaException;
import com.example.flightdemo.repository.SeatReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatReservationService {

    private final SeatReservationRepository seatRepository;

    public void reserve(String sagaId, String userId, String flightId, String seatId) {
        try {
            // Simulates seat already taken — triggers saga compensation
            if (seatId.toUpperCase().startsWith("TAKEN")) {
                throw new SagaException("SEAT_RESERVATION", "Seat %s on flight %s is already reserved".formatted(seatId, flightId));
            }
            seatRepository.save(SeatReservation.builder()
                .sagaId(sagaId).userId(userId).flightId(flightId).seatId(seatId).status("RESERVED")
                .build());
            log.info("[Seat] Reserved seat {} on flight {}", seatId, flightId);
        } catch (SagaException e) {
            throw e;


        } catch (Exception e) {
            throw new SagaException("SEAT_RESERVATION", "Seat reservation failed: %s".formatted(e.getMessage()));
        }
    }

    // Compensating transaction — releases the seat if a later step fails
    public void release(String sagaId) {
        seatRepository.findBySagaId(sagaId).ifPresent(reservation -> {
            reservation.setStatus("RELEASED");
            seatRepository.save(reservation);
            log.info("[Seat] Released seat {} for saga {}", reservation.getSeatId(), sagaId);
        });
    }
}
