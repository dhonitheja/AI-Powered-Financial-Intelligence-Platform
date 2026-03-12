package com.wealthix.plaid.model;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class PlaidExchangeRequest {
    @NotBlank
    private String publicToken;
}
