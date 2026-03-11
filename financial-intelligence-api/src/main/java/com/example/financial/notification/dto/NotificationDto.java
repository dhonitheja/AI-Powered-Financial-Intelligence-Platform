package com.example.financial.notification.dto;

import com.example.financial.notification.entity.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationDto {
    private UUID id;
    private String message;
    private String type;
    private boolean isRead;
    private LocalDateTime createdAt;

    public NotificationDto() {}

    public NotificationDto(Notification n) {
        this.id = n.getId();
        this.message = n.getMessage();
        this.type = n.getType().name();
        this.isRead = n.isRead();
        this.createdAt = n.getCreatedAt();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
