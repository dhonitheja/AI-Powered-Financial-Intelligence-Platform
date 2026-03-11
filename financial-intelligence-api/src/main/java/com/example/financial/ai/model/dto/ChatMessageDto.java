package com.example.financial.ai.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatMessageDto {

    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    private String message;

    public ChatMessageDto() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
