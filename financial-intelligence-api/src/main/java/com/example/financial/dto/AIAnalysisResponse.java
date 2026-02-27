package com.example.financial.dto;

public class AIAnalysisResponse {
    private String category;
    private Double fraudRiskScore;
    private String explanation;

    public AIAnalysisResponse() {
    }

    public AIAnalysisResponse(String category, Double fraudRiskScore, String explanation) {
        this.category = category;
        this.fraudRiskScore = fraudRiskScore;
        this.explanation = explanation;
    }

    public static class AIAnalysisResponseBuilder {
        private String category;
        private Double fraudRiskScore;
        private String explanation;

        public AIAnalysisResponseBuilder category(String category) {
            this.category = category;
            return this;
        }

        public AIAnalysisResponseBuilder fraudRiskScore(Double fraudRiskScore) {
            this.fraudRiskScore = fraudRiskScore;
            return this;
        }

        public AIAnalysisResponseBuilder explanation(String explanation) {
            this.explanation = explanation;
            return this;
        }

        public AIAnalysisResponse build() {
            return new AIAnalysisResponse(category, fraudRiskScore, explanation);
        }
    }

    public static AIAnalysisResponseBuilder builder() {
        return new AIAnalysisResponseBuilder();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getFraudRiskScore() {
        return fraudRiskScore;
    }

    public void setFraudRiskScore(Double fraudRiskScore) {
        this.fraudRiskScore = fraudRiskScore;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
