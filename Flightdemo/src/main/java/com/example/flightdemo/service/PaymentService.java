package com.example.flightdemo.service;

import com.example.flightdemo.entity.Payment;
import com.example.flightdemo.exception.SagaException;
import com.example.flightdemo.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public void charge(String sagaId, String userId, double amount) {
        try {
            // Simulates payment gateway rejection — triggers saga compensation
            if (userId.toUpperCase().startsWith("PAY_FAIL")) {
                throw new SagaException("PAYMENT", "Payment gateway rejected: insufficient funds for user %s".formatted(userId));
            }
            paymentRepository.save(Payment.builder()
                .sagaId(sagaId).userId(userId).amount(amount).status("CHARGED")
                .build());
            log.info("[Payment] Charged ${} for user {}", amount, userId);
        } catch (SagaException e) {
            throw e;
        } catch (Exception e) {
            throw new SagaException("PAYMENT", "Payment charge failed: %s".formatted(e.getMessage()));
        }
    }

    // Compensating transaction — automatically triggered if a later step fails
    public void refund(String sagaId) {
        paymentRepository.findBySagaId(sagaId).ifPresent(payment -> {
            payment.setStatus("REFUNDED");
            paymentRepository.save(payment);
            log.info("[Payment] Refunded ${} for saga {}", payment.getAmount(), sagaId);
        });
    }
}
