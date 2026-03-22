package com.wealthix.ai.model.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AITransactionDTO {
    private String id;
    private Double amount;
    private String date;
    private String description;
    private String category;
    private String merchant;
    private Boolean pending;
    private String accountSubtype;
    private Map<String, String> location;
}
