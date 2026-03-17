package com.wealthix.repository;

import com.wealthix.entity.UserBankConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface UserBankConnectionRepository extends JpaRepository<UserBankConnection, UUID> {

    List<UserBankConnection> findByUserId(UUID userId);

    List<UserBankConnection> findByItemId(String itemId);

    boolean existsByPlaidAccountIdAndUserId(String plaidAccountId, UUID userId);

    Optional<UserBankConnection> findByPlaidAccountIdAndUserId(String plaidAccountId, UUID userId);

    Optional<UserBankConnection> findByIdAndUserId(UUID id, UUID userId);
}
