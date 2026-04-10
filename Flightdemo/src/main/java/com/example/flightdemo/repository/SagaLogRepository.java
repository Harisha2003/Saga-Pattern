package com.example.flightdemo.repository;

import com.example.flightdemo.entity.SagaLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SagaLogRepository extends JpaRepository<SagaLog, Long> {
    List<SagaLog> findBySagaId(String sagaId);
    Optional<SagaLog> findBySagaIdAndStepName(String sagaId, String stepName);
}
