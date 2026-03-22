package com.wealthix.ai.model.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JassHybridResponseDTO {
    // Standard Analysis (from Gemini 1.5 Flash)
    @JsonProperty("analysis_id")
    private String analysisId;
    
    @JsonProperty("spending_velocity")
    private Double spendingVelocity;
    
    @JsonProperty("health_score")
    private Integer healthScore;
    
    @JsonProperty("ghost_subscriptions")
    private List<String> ghostSubscriptions;
    
    @JsonProperty("standard_report")
    private String standardReport;

    // Expert Insights (from Claude 3.5 Sonnet - Optional)
    @JsonProperty("expert_advice")
    private String expertAdvice; 
    
    @JsonProperty("tax_strategy")
    private Map<String, Object> taxStrategy;
    
    @JsonProperty("requires_expert_followup")
    private boolean requiresExpertFollowup;

    @JsonProperty("model_used")
    private String modelUsed;

    @JsonProperty("complexity_score")
    private Integer complexityScore;

    // Helper to check if Claude was triggered
    public boolean hasExpertInsight() {
        return expertAdvice != null && !expertAdvice.isEmpty();
    }
}
