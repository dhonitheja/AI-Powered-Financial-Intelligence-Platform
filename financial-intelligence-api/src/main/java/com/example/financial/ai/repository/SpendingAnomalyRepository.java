package com.example.financial.ai.repository;

import com.example.financial.ai.model.entity.SpendingAnomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpendingAnomalyRepository extends JpaRepository<SpendingAnomaly, UUID> {
    List<SpendingAnomaly> findByUserIdAndIsAcknowledgedFalse(UUID userId);
}
