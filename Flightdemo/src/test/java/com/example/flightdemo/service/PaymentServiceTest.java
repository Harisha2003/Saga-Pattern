package com.example.flightdemo.service;

import com.example.flightdemo.entity.Payment;
import com.example.flightdemo.exception.SagaException;
import com.example.flightdemo.repository.PaymentRepository;
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
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @InjectMocks PaymentService paymentService;

    // ─────────────────────────────────────────────────────────
    // TC01: charge() saves payment with CHARGED status
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC01 - charge: saves payment record with CHARGED status")
    void tc01_charge_savesPaymentWithChargedStatus() {
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        paymentService.charge("saga-1", "USER_001", 299.99);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());

        Payment saved = captor.getValue();
        assertThat(saved.getSagaId()).isEqualTo("saga-1");
        assertThat(saved.getUserId()).isEqualTo("USER_001");
        assertThat(saved.getAmount()).isEqualTo(299.99);
        assertThat(saved.getStatus()).isEqualTo("CHARGED");
    }

    // ─────────────────────────────────────────────────────────
    // TC02: charge() is case-insensitive for PAY_FAIL trigger
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC03 - charge: PAY_FAIL trigger is case-insensitive")
    void tc03_charge_caseInsensitiveTrigger() {
        assertThrows(SagaException.class,
                () -> paymentService.charge("saga-1", "PAY_FAIL_USER", 100.00));
        assertThrows(SagaException.class,
                () -> paymentService.charge("saga-1", "Pay_Fail_User", 100.00));
    }


    // ─────────────────────────────────────────────────────────
    // TC03: refund() does nothing if no payment found
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC05 - refund: does nothing if no payment record found")
    void tc05_refund_doesNothing_ifNoPaymentFound() {
        when(paymentRepository.findBySagaId("saga-999")).thenReturn(Optional.empty());

        paymentService.refund("saga-999");

        verify(paymentRepository, never()).save(any());
    }
}
