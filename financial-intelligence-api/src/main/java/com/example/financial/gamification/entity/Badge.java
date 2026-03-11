package com.example.financial.gamification.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "badges")
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private String icon; // string representing icon, e.g. "Flame", "Star"

    @Column(nullable = false, name = "required_points")
    private Integer requiredPoints; // Points required to earn

    public Badge() {}

    public Badge(String name, String description, String icon, Integer requiredPoints) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.requiredPoints = requiredPoints;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Integer getRequiredPoints() { return requiredPoints; }
    public void setRequiredPoints(Integer requiredPoints) { this.requiredPoints = requiredPoints; }
}
