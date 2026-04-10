package com.example.flightdemo.orchestrator;

import com.example.flightdemo.dto.BookingRequest;
import com.example.flightdemo.entity.SagaLog;
import com.example.flightdemo.exception.SagaException;
import com.example.flightdemo.repository.SagaLogRepository;
import com.example.flightdemo.service.LoyaltyPointsService;
import com.example.flightdemo.service.PaymentService;
import com.example.flightdemo.service.SeatReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingOrchestratorTest {

    @Mock PaymentService paymentService;
    @Mock SeatReservationService seatService;
    @Mock LoyaltyPointsService loyaltyService;
    @Mock SagaLogRepository sagaLogRepository;

    @InjectMocks BookingOrchestrator orchestrator;

    private BookingRequest request;

    @BeforeEach
    void setUp() {
        request = new BookingRequest("USER_001", "FL100", "12A", 299.99);
        when(sagaLogRepository.findBySagaIdAndStepName(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(sagaLogRepository.save(any(SagaLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ─────────────────────────────────────────────────────────
    // TC01: Full success — all 3 steps execute in correct order
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC01 - Full success: all steps execute in correct order")
    void tc01_fullSuccess_allStepsInOrder() {
        String sagaId = orchestrator.book(request);

        assertThat(sagaId).isNotNull();

        InOrder inOrder = inOrder(seatService, paymentService, loyaltyService);
        inOrder.verify(seatService).reserve(eq(sagaId), eq("USER_001"), eq("FL100"), eq("12A"));
        inOrder.verify(paymentService).charge(eq(sagaId), eq("USER_001"), eq(299.99));
        inOrder.verify(loyaltyService).addPoints(eq(sagaId), eq("USER_001"), eq(100));
    }

    // ─────────────────────────────────────────────────────────
    // TC02: Full success — no compensations triggered
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC02 - Full success: no compensating transactions called")
    void tc02_fullSuccess_noCompensations() {
        orchestrator.book(request);

        verify(seatService, never()).release(anyString());
        verify(paymentService, never()).refund(anyString());
        verify(loyaltyService, never()).reversePoints(anyString());
    }

    // ─────────────────────────────────────────────────────────
    // TC03: Seat fails at Step 1 — nothing to compensate
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC03 - Seat fails at step 1: no compensations run")
    void tc03_seatFails_noCompensations() {
        doThrow(new SagaException("SEAT_RESERVATION", "Seat already taken"))
                .when(seatService).reserve(anyString(), anyString(), anyString(), anyString());

        SagaException ex = assertThrows(SagaException.class, () -> orchestrator.book(request));

        assertThat(ex.getFailedStep()).isEqualTo("SEAT_RESERVATION");

        // payment and loyalty never ran
        verify(paymentService, never()).charge(anyString(), anyString(), anyDouble());
        verify(loyaltyService, never()).addPoints(anyString(), anyString(), anyInt());

        // nothing to compensate
        verify(seatService, never()).release(anyString());
        verify(paymentService, never()).refund(anyString());
        verify(loyaltyService, never()).reversePoints(anyString());
    }

    // ─────────────────────────────────────────────────────────
    // TC04: Payment fails at Step 2 — seat released
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC04 - Payment fails: seat released as compensation")
    void tc04_paymentFails_seatReleased() {
        doThrow(new SagaException("PAYMENT", "Insufficient funds"))
                .when(paymentService).charge(anyString(), anyString(), anyDouble());

        SagaException ex = assertThrows(SagaException.class, () -> orchestrator.book(request));

        assertThat(ex.getFailedStep()).isEqualTo("PAYMENT");

        // seat was reserved — must be released
        verify(seatService).release(anyString());

        // loyalty never ran — no compensation
        verify(loyaltyService, never()).addPoints(anyString(), anyString(), anyInt());
        verify(loyaltyService, never()).reversePoints(anyString());
        verify(paymentService, never()).refund(anyString());
    }

    // ─────────────────────────────────────────────────────────
    // TC05: Loyalty fails at Step 3 — payment refunded + seat released
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC05 - Loyalty fails: payment refunded and seat released")
    void tc05_loyaltyFails_paymentRefunded_seatReleased() {
        doThrow(new SagaException("LOYALTY_POINTS", "DB constraint violation"))
                .when(loyaltyService).addPoints(anyString(), anyString(), anyInt());

        SagaException ex = assertThrows(SagaException.class, () -> orchestrator.book(request));

        assertThat(ex.getFailedStep()).isEqualTo("LOYALTY_POINTS");

        // both must be compensated
        verify(paymentService).refund(anyString());
        verify(seatService).release(anyString());
        verify(loyaltyService, never()).reversePoints(anyString());
    }

    // ─────────────────────────────────────────────────────────
    // TC06: Compensation runs in reverse order
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC06 - Loyalty fails: compensation runs in reverse order (payment first, seat second)")
    void tc06_loyaltyFails_compensationInReverseOrder() {
        doThrow(new SagaException("LOYALTY_POINTS", "DB constraint violation"))
                .when(loyaltyService).addPoints(anyString(), anyString(), anyInt());

        assertThrows(SagaException.class, () -> orchestrator.book(request));

        // reverse order: PAYMENT refund first, then SEAT release
        InOrder inOrder = inOrder(paymentService, seatService);
        inOrder.verify(paymentService).refund(anyString());
        inOrder.verify(seatService).release(anyString());
    }

    // ─────────────────────────────────────────────────────────
    // TC07: Each booking generates a unique sagaId
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC07 - Each booking generates a unique sagaId")
    void tc07_uniqueSagaIdPerBooking() {
        String sagaId1 = orchestrator.book(request);
        String sagaId2 = orchestrator.book(
                new BookingRequest("USER_002", "FL200", "5B", 150.00));

        assertThat(sagaId1).isNotEqualTo(sagaId2);
    }



    // ─────────────────────────────────────────────────────────
    // TC10: SagaLog saved for all steps on success
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC10 - SagaLog saved for all 3 steps on success")
    void tc10_sagaLog_savedForAllSteps() {
        orchestrator.book(request);

        // 3 steps — each gets a log entry
        verify(sagaLogRepository, atLeast(3)).save(any(SagaLog.class));
    }
}
