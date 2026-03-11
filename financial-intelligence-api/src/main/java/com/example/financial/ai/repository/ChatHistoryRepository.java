package com.example.financial.ai.repository;

import com.example.financial.ai.model.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, UUID> {
    List<ChatHistory> findTop20ByUserIdAndSessionIdOrderByCreatedAtDesc(UUID userId, UUID sessionId);
}
