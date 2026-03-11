package com.example.financial.gamification.dto;

import com.example.financial.gamification.entity.Badge;
import java.util.UUID;

public class BadgeDto {
    private UUID id;
    private String name;
    private String description;
    private String icon;

    public BadgeDto() {}

    public BadgeDto(Badge badge) {
        this.id = badge.getId();
        this.name = badge.getName();
        this.description = badge.getDescription();
        this.icon = badge.getIcon();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
