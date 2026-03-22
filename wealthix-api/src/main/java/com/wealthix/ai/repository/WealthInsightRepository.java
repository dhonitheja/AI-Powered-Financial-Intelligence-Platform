package com.wealthix.ai.repository;

import com.wealthix.ai.model.entity.WealthInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WealthInsightRepository extends JpaRepository<WealthInsight, UUID> {
    Optional<WealthInsight> findTopByUserIdOrderByCreatedAtDesc(UUID userId);
}
