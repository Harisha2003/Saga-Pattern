package com.example.flightdemo.service;

import com.example.flightdemo.entity.LoyaltyPoints;
import com.example.flightdemo.exception.SagaException;
import com.example.flightdemo.repository.LoyaltyPointsRepository;
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
class LoyaltyPointsServiceTest {

    @Mock LoyaltyPointsRepository loyaltyRepository;
    @InjectMocks LoyaltyPointsService loyaltyService;

    // ─────────────────────────────────────────────────────────
    // TC01: addPoints() saves loyalty record with AWARDED status
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC01 - addPoints: saves loyalty record with AWARDED status")
    void tc01_addPoints_savesWithAwardedStatus() {
        when(loyaltyRepository.save(any(LoyaltyPoints.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        loyaltyService.addPoints("saga-1", "USER_001", 100);

        ArgumentCaptor<LoyaltyPoints> captor = ArgumentCaptor.forClass(LoyaltyPoints.class);
        verify(loyaltyRepository).save(captor.capture());

        LoyaltyPoints saved = captor.getValue();
        assertThat(saved.getSagaId()).isEqualTo("saga-1");
        assertThat(saved.getUserId()).isEqualTo("USER_001");
        assertThat(saved.getPoints()).isEqualTo(100);
        assertThat(saved.getStatus()).isEqualTo("AWARDED");
    }


    // ─────────────────────────────────────────────────────────
    // TC03: addPoints() is case-insensitive for ERR trigger
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC03 - addPoints: ERR trigger is case-insensitive")
    void tc03_addPoints_caseInsensitiveTrigger() {
        assertThrows(SagaException.class,
                () -> loyaltyService.addPoints("saga-1", "ERR_USER", 100));
        assertThrows(SagaException.class,
                () -> loyaltyService.addPoints("saga-1", "Err_User", 100));
    }

    // ─────────────────────────────────────────────────────────
    // TC04: reversePoints() updates loyalty status to REVERSED
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC04 - reversePoints: updates loyalty status to REVERSED")
    void tc04_reversePoints_updatesStatusToReversed() {
        LoyaltyPoints existing = LoyaltyPoints.builder()
                .sagaId("saga-1").userId("USER_001").points(100).status("AWARDED")
                .build();
        when(loyaltyRepository.findBySagaId("saga-1")).thenReturn(Optional.of(existing));
        when(loyaltyRepository.save(any(LoyaltyPoints.class))).thenAnswer(inv -> inv.getArgument(0));

        loyaltyService.reversePoints("saga-1");

        ArgumentCaptor<LoyaltyPoints> captor = ArgumentCaptor.forClass(LoyaltyPoints.class);
        verify(loyaltyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("REVERSED");
    }

    // ─────────────────────────────────────────────────────────
    // TC05: reversePoints() does nothing if no loyalty record found
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC05 - reversePoints: does nothing if no loyalty record found")
    void tc05_reversePoints_doesNothing_ifNoRecordFound() {
        when(loyaltyRepository.findBySagaId("saga-999")).thenReturn(Optional.empty());

        loyaltyService.reversePoints("saga-999");

        verify(loyaltyRepository, never()).save(any());
    }
}
