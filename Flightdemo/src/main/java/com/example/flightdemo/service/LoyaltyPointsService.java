package com.example.flightdemo.service;

import com.example.flightdemo.entity.LoyaltyPoints;
import com.example.flightdemo.exception.SagaException;
import com.example.flightdemo.repository.LoyaltyPointsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyPointsService {

    private final LoyaltyPointsRepository loyaltyRepository;

    public void addPoints(String sagaId, String userId, int points) {
        // Simulates a DB constraint violation — triggers saga compensation
        if (userId.toUpperCase().startsWith("ERR")) {
            throw new SagaException("LOYALTY_POINTS",
                "DB constraint violation: duplicate loyalty entry for user %s".formatted(userId));
        }
        loyaltyRepository.save(LoyaltyPoints.builder()
            .sagaId(sagaId).userId(userId).points(points).status("AWARDED")
            .build());
        log.info("[Loyalty] Awarded {} points to user {}", points, userId);
    }

    // Compensating transaction — reverses points if already awarded
    public void reversePoints(String sagaId) {
        loyaltyRepository.findBySagaId(sagaId).ifPresent(loyalty -> {
            loyalty.setStatus("REVERSED");
            loyaltyRepository.save(loyalty);
            log.info("[Loyalty] Reversed {} points for saga {}", loyalty.getPoints(), sagaId);
        });
    }
}
