package com.wealthix.plaid.model;

import com.wealthix.entity.UserBankConnection;
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
    private String accountSubtype;
    private String accountMask;
    private Double currentBalance;

    public static UserBankConnectionResponse fromEntity(UserBankConnection entity) {
        return UserBankConnectionResponse.builder()
                .id(entity.getId())
                .institutionName(entity.getInstitutionName())
                .accountName(entity.getAccountName())
                .accountType(entity.getAccountType())
                .accountSubtype(entity.getAccountSubtype())
                .accountMask(entity.getAccountMask())
                .currentBalance(entity.getCurrentBalance())
                .build();
    }
}
