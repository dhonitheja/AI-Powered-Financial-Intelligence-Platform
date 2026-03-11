package com.example.financial.gamification.entity;

import com.example.financial.entity.AppUser;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "user_scores")
public class UserScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(nullable = false)
    private Integer points = 0;

    @Column(nullable = false)
    private Integer level = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tier tier = Tier.BRONZE;

    public UserScore() {}

    public UserScore(AppUser user) {
        this.user = user;
        this.points = 0;
        this.level = 1;
        this.tier = Tier.BRONZE;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Tier getTier() { return tier; }
    public void setTier(Tier tier) { this.tier = tier; }
}
