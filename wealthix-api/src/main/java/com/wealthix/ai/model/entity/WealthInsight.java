package com.wealthix.ai.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wealth_insights")
@Data
@NoArgsConstructor
public class WealthInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(columnDefinition = "TEXT")
    private String wealthTip;

    @Column(name = "spending_velocity")
    private String spendingVelocity;

    @Column(name = "burn_rate")
    private String burnRate;

    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "ghost_subscriptions")
    private Integer ghostSubscriptions;

    @Column(name = "analysis_id")
    private UUID analysisId;

    @Column(name = "financial_health_score")
    private Integer financialHealthScore;

    @Column(name = "daily_burn_rate")
    private Double dailyBurnRate;

    @Column(name = "savings_rate_change")
    private String savingsRateChange;

    @Column(name = "tax_deductible_estimate")
    private Double taxDeductibleEstimate;

    @Column(name = "expert_advice", columnDefinition = "TEXT")
    private String expertAdvice;

    @Column(name = "model_used")
    private String modelUsed;

    @Column(name = "router_confidence_score")
    private Integer routerConfidenceScore;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
