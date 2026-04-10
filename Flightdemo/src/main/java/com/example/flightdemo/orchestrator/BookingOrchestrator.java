package com.example.flightdemo.orchestrator;

import com.example.flightdemo.dto.BookingRequest;
import com.example.flightdemo.entity.SagaLog;
import com.example.flightdemo.entity.SagaLog.StepStatus;
import com.example.flightdemo.exception.SagaException;
import com.example.flightdemo.repository.SagaLogRepository;
import com.example.flightdemo.service.LoyaltyPointsService;
import com.example.flightdemo.service.PaymentService;
import com.example.flightdemo.service.SeatReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingOrchestrator {

    private record SagaStep(String name, Runnable execute, Runnable compensate) {}

    private final PaymentService paymentService;
    private final SeatReservationService seatService;
    private final LoyaltyPointsService loyaltyService;
    private final SagaLogRepository sagaLogRepository;

    private List<SagaStep> buildSteps(String sagaId, BookingRequest req) {
        return List.of(
            new SagaStep(
                "SEAT_RESERVATION",
                () -> seatService.reserve(sagaId, req.userId(), req.flightId(), req.seatId()),
                () -> seatService.release(sagaId)
            ),
            new SagaStep(
                "PAYMENT",
                () -> paymentService.charge(sagaId, req.userId(), req.amount()),
                () -> paymentService.refund(sagaId)
            ),
            new SagaStep(
                "LOYALTY_POINTS",
                () -> loyaltyService.addPoints(sagaId, req.userId(), 100),
                () -> loyaltyService.reversePoints(sagaId)
            )
        );
    }

    public String book(BookingRequest request) {
        String sagaId = UUID.randomUUID().toString();
        List<SagaStep> steps = buildSteps(sagaId, request);
        List<SagaStep> completed = new ArrayList<>();

        try {
            for (SagaStep step : steps) {
                log(sagaId, step.name(), StepStatus.STARTED);
                step.execute().run();
                log(sagaId, step.name(), StepStatus.COMPLETED);
                completed.add(step);
            }
            return sagaId;

        } catch (SagaException e) {
            // Known business failure — step name is available
            log.error("[Orchestrator] Business failure at step: {} — {}", e.getFailedStep(), e.getMessage());
            log(sagaId, e.getFailedStep(), StepStatus.FAILED);
            runCompensations(sagaId, completed);
            throw e;

        } catch (Exception e) {
            // Unknown failure — DB down, NPE, network error, etc.
            // Derive the failed step from the last STARTED entry in completed tracking
            String failedStep = resolveFailedStep(sagaId, completed, steps);
            log.error("[Orchestrator] Unexpected failure at step: {} — {}", failedStep, e.getMessage());
            log(sagaId, failedStep, StepStatus.FAILED);
            runCompensations(sagaId, completed);
            throw new SagaException(failedStep, "Unexpected error: %s".formatted(e.getMessage()));
        }
    }

    private void runCompensations(String sagaId, List<SagaStep> completed) {
        List<SagaStep> toCompensate = new ArrayList<>(completed);
        for (int i = toCompensate.size() - 1; i >= 0; i--) {
            SagaStep step = toCompensate.get(i);
            try {
                log.info("[Orchestrator] Compensating: {}", step.name());
                step.compensate().run();
                log(sagaId, step.name(), StepStatus.COMPENSATED);
            } catch (Exception ex) {
                // Compensation itself failed — log and continue to next step
                log.error("[Orchestrator] Compensation failed for step: {} — {}", step.name(), ex.getMessage());
            }
        }
    }

    private String resolveFailedStep(String sagaId, List<SagaStep> completed, List<SagaStep> allSteps) {
        // The failed step is the one right after the last completed step
        int nextIndex = completed.size();
        return nextIndex < allSteps.size() ? allSteps.get(nextIndex).name() : "UNKNOWN";
    }

    private void log(String sagaId, String step, StepStatus status) {
        sagaLogRepository.findBySagaIdAndStepName(sagaId, step).ifPresentOrElse(
            existing -> {
                existing.setStatus(status);
                sagaLogRepository.save(existing);
            },
            () -> sagaLogRepository.save(new SagaLog(sagaId, step, status))
        );
    }
}
