package com.example.flightdemo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Access(AccessType.FIELD)
public class SagaLog {

    public enum StepStatus { STARTED, COMPLETED, COMPENSATED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sagaId;
    private String stepName;

    @Enumerated(EnumType.STRING)
    private StepStatus status;

    private LocalDateTime updatedAt;
//initialize the new log with current time
    public SagaLog(String sagaId, String stepName, StepStatus status) {
        this.sagaId = sagaId;
        this.stepName = stepName;
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    // Override Lombok's generated setStatus to also update updatedAt
    public void setStatus(StepStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}
