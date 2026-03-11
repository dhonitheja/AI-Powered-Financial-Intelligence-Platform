package com.example.financial.ai.model.dto;

import java.util.List;

public class ChatResponseDto {
    private String reply;
    private String sessionId;
    private List<String> suggestedActions;

    public ChatResponseDto() {}

    public ChatResponseDto(String reply, String sessionId, List<String> suggestedActions) {
        this.reply = reply;
        this.sessionId = sessionId;
        this.suggestedActions = suggestedActions;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public List<String> getSuggestedActions() { return suggestedActions; }
    public void setSuggestedActions(List<String> suggestedActions) { this.suggestedActions = suggestedActions; }
}
