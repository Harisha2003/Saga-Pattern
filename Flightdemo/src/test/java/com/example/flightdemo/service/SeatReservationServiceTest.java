package com.example.flightdemo.service;

import com.example.flightdemo.entity.SeatReservation;
import com.example.flightdemo.exception.SagaException;
import com.example.flightdemo.repository.SeatReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatReservationServiceTest {

    @Mock SeatReservationRepository seatRepository;
    @InjectMocks SeatReservationService seatService;

    // ─────────────────────────────────────────────────────────
    // TC01: reserve() saves seat with RESERVED status
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC01 - reserve: saves seat record with RESERVED status")
    void tc01_reserve_savesSeatWithReservedStatus() {
        when(seatRepository.save(any(SeatReservation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        seatService.reserve("saga-1", "USER_001", "FL100", "12A");

        ArgumentCaptor<SeatReservation> captor = ArgumentCaptor.forClass(SeatReservation.class);
        verify(seatRepository).save(captor.capture());

        SeatReservation saved = captor.getValue();
        assertThat(saved.getSagaId()).isEqualTo("saga-1");
        assertThat(saved.getUserId()).isEqualTo("USER_001");
        assertThat(saved.getFlightId()).isEqualTo("FL100");
        assertThat(saved.getSeatId()).isEqualTo("12A");
        assertThat(saved.getStatus()).isEqualTo("RESERVED");
    }

    // ─────────────────────────────────────────────────────────
    // TC02: reserve() throws SagaException for TAKEN seatId
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC02 - reserve: throws SagaException for TAKEN seatId")
    void tc02_reserve_throwsSagaException_forTakenSeat() {
        SagaException ex = assertThrows(SagaException.class,
                () -> seatService.reserve("saga-1", "USER_001", "FL100", "taken_12A"));

        assertThat(ex.getFailedStep()).isEqualTo("SEAT_RESERVATION");
        assertThat(ex.getMessage()).contains("already reserved");

        verify(seatRepository, never()).save(any());
    }



    // ─────────────────────────────────────────────────────────
    // TC04: release() updates seat status to RELEASED
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC04 - release: updates seat status to RELEASED")
    void tc04_release_updatesStatusToReleased() {
        SeatReservation existing = SeatReservation.builder()
                .sagaId("saga-1").userId("USER_001").flightId("FL100")
                .seatId("12A").status("RESERVED").build();
        when(seatRepository.findBySagaId("saga-1")).thenReturn(Optional.of(existing));
        when(seatRepository.save(any(SeatReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        seatService.release("saga-1");

        ArgumentCaptor<SeatReservation> captor = ArgumentCaptor.forClass(SeatReservation.class);
        verify(seatRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("RELEASED");
    }

    // ─────────────────────────────────────────────────────────
    // TC05: release() does nothing if no seat found
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC05 - release: does nothing if no seat record found")
    void tc05_release_doesNothing_ifNoSeatFound() {
        when(seatRepository.findBySagaId("saga-999")).thenReturn(Optional.empty());

        seatService.release("saga-999");

        verify(seatRepository, never()).save(any());
    }
}
