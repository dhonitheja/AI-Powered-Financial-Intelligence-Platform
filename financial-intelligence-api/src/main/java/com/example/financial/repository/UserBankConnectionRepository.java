package com.example.financial.repository;

import com.example.financial.entity.UserBankConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserBankConnectionRepository extends JpaRepository<UserBankConnection, UUID> {

    /**
     * Returns all connections for a given user (may span multiple items/accounts).
     */
    List<UserBankConnection> findByUserId(String userId);

    /**
     * Returns all connections that share a given Plaid itemId.
     * Used by the webhook handler to identify affected users without exposing
     * userIds externally.
     */
    List<UserBankConnection> findByItemId(String itemId);
}
