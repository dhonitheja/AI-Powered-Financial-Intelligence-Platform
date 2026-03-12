package com.example.financial.repository;

import com.example.financial.entity.UserBankConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserBankConnectionRepository extends JpaRepository<UserBankConnection, UUID> {

    List<UserBankConnection> findByUserId(UUID userId);

    List<UserBankConnection> findByItemId(String itemId);

    boolean existsByPlaidAccountIdAndUserId(String plaidAccountId, UUID userId);

    java.util.Optional<UserBankConnection> findByIdAndUserId(UUID id, UUID userId);
}
