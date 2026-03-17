package com.wealthix.repository;

import com.wealthix.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findAllByUserIdAndDeletedFalse(UUID userId);
    Optional<ApiKey> findByApiKeyHashedAndDeletedFalseAndActiveTrue(String hashedKey);
}
