package com.wealthix.plaid.model;

import com.example.financial.entity.UserBankConnection;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class UserBankConnectionResponse {
    private UUID id;
    private String institutionName;
    private String accountName;
    private String accountType;
    private String accountMask;
    private Double currentBalance;

    public static UserBankConnectionResponse fromEntity(UserBankConnection entity) {
        return UserBankConnectionResponse.builder()
                .id(entity.getId())
                .institutionName(entity.getInstitutionName())
                .accountName(entity.getAccountName())
                .accountType(entity.getAccountType())
                // .accountMask(entity.getAccountMask()) // Add if entity updated
                .currentBalance(entity.getCurrentBalance())
                .build();
    }
}
