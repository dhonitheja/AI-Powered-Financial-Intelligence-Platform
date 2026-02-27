package com.example.financial.dto;

import java.util.List;

public class AssistantResponse {
    private String answer;
    private Double confidenceScore;
    private List<String> suggestions;

    public AssistantResponse() {
    }

    public AssistantResponse(String answer, Double confidenceScore, List<String> suggestions) {
        this.answer = answer;
        this.confidenceScore = confidenceScore;
        this.suggestions = suggestions;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
}
