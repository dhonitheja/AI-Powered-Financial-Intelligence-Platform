package com.example.financial.gamification.repository;

import com.example.financial.gamification.entity.UserScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserScoreRepository extends JpaRepository<UserScore, UUID> {
    Optional<UserScore> findByUserId(UUID userId);
}
