package com.example.financial.gamification.dto;

import java.util.List;

public class GamificationProfileDto {
    private Integer points;
    private Integer level;
    private String tier;
    private List<BadgeDto> badges;
    private Integer nextLevelPoints;

    public GamificationProfileDto() {}

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public List<BadgeDto> getBadges() { return badges; }
    public void setBadges(List<BadgeDto> badges) { this.badges = badges; }

    public Integer getNextLevelPoints() { return nextLevelPoints; }
    public void setNextLevelPoints(Integer nextLevelPoints) { this.nextLevelPoints = nextLevelPoints; }
}
