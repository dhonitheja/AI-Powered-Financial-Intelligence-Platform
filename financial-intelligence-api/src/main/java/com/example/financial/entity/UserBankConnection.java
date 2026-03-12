package com.example.financial.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "user_bank_connections")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UserBankConnection extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String institutionId;

    @Column(nullable = false)
    private String institutionName;

    @Column(name = "access_token", nullable = false)
    private String encryptedAccessToken;

    @Column(unique = true)
    private String itemId;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "account_official_name")
    private String accountOfficialName;

    @Column(name = "account_type", length = 50)
    private String accountType;

    @Column(name = "account_subtype", length = 50)
    private String accountSubtype;

    @Column(name = "account_mask", length = 10)
    private String accountMask;

    @Builder.Default
    @Column(nullable = false)
    private boolean isPending = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean isDeleted = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean isManual = false;
    
    @Column(name = "plaid_account_id")
    private String plaidAccountId;

    @Column(name = "current_balance")
    private Double currentBalance;

    @Column(name = "credit_limit")
    private Double creditLimit;

    @Column(name = "account_name")
    private String accountName;
}
