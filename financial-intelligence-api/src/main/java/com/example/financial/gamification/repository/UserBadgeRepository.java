package com.example.financial.gamification.repository;

import com.example.financial.gamification.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {
    List<UserBadge> findByUserId(UUID userId);
    boolean existsByUserIdAndBadgeId(UUID userId, UUID badgeId);
}
