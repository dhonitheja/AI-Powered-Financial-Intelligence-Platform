package com.wealthix.ai.model.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
public class AIAnalysisRequestDTO {
    @JsonProperty("user_id")
    private String userId;
    private List<AITransactionDTO> transactions;
}
