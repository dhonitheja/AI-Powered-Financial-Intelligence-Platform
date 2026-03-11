package com.example.financial.gamification.service;

import com.example.financial.entity.AppUser;
import com.example.financial.gamification.dto.BadgeDto;
import com.example.financial.gamification.dto.GamificationProfileDto;
import com.example.financial.gamification.entity.Badge;
import com.example.financial.gamification.entity.Tier;
import com.example.financial.gamification.entity.UserBadge;
import com.example.financial.gamification.entity.UserScore;
import com.example.financial.gamification.repository.BadgeRepository;
import com.example.financial.gamification.repository.UserBadgeRepository;
import com.example.financial.gamification.repository.UserScoreRepository;
import com.example.financial.notification.entity.NotificationType;
import com.example.financial.notification.service.NotificationService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GamificationService {

    private final UserScoreRepository userScoreRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final NotificationService notificationService;

    public GamificationService(UserScoreRepository userScoreRepository,
                               BadgeRepository badgeRepository,
                               UserBadgeRepository userBadgeRepository,
                               NotificationService notificationService) {
        this.userScoreRepository = userScoreRepository;
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.notificationService = notificationService;
    }

    @PostConstruct
    public void seedBadges() {
        if (badgeRepository.count() == 0) {
            badgeRepository.save(new Badge("First Login", "Logged into Wealthix for the first time.", "🔑", 0));
            badgeRepository.save(new Badge("Savings Novice", "Earned 100 points.", "🌱", 100));
            badgeRepository.save(new Badge("Financial Wizard", "Earned 500 points. Reached Silver Tier!", "✨", 500));
            badgeRepository.save(new Badge("Budget Master", "Earned 2000 points. Gold Tier!", "🏆", 2000));
        }
    }

    @Transactional
    public void awardPoints(AppUser user, int pointsToAdd, String reason) {
        UserScore score = userScoreRepository.findByUserId(user.getId())
                .orElse(new UserScore(user));

        int newPoints = score.getPoints() + pointsToAdd;
        score.setPoints(newPoints);

        // Calculate Level (100 pts per level)
        int newLevel = (newPoints / 100) + 1;
        if (newLevel > score.getLevel()) {
            score.setLevel(newLevel);
            notificationService.sendNotification(user.getEmail(),
                    "You reached Level " + newLevel + "!", NotificationType.SUCCESS);
        }

        // Calculate Tier
        Tier oldTier = score.getTier();
        Tier newTier = calculateTier(newPoints);
        if (newTier != oldTier) {
            score.setTier(newTier);
            notificationService.sendNotification(user.getEmail(),
                    "Congratulations! You've been upgraded to " + newTier + " Tier!", NotificationType.SUCCESS);
        }

        userScoreRepository.save(score);

        // Check for new badges
        checkForNewBadges(user, score.getPoints());
    }

    private void checkForNewBadges(AppUser user, int currentPoints) {
        List<Badge> eligibleBadges = badgeRepository.findByRequiredPointsLessThanEqual(currentPoints);
        for (Badge badge : eligibleBadges) {
            if (!userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId())) {
                UserBadge userBadge = new UserBadge(user, badge);
                userBadgeRepository.save(userBadge);
                notificationService.sendNotification(user.getEmail(),
                        "You earned a new badge: " + badge.getName() + " " + badge.getIcon(),
                        NotificationType.SUCCESS);
            }
        }
    }

    private Tier calculateTier(int points) {
        if (points >= 10000) return Tier.DIAMOND;
        if (points >= 5000) return Tier.PLATINUM;
        if (points >= 2000) return Tier.GOLD;
        if (points >= 500) return Tier.SILVER;
        return Tier.BRONZE;
    }

    public GamificationProfileDto getProfile(AppUser user) {
        UserScore score = userScoreRepository.findByUserId(user.getId())
                .orElse(new UserScore(user));

        List<BadgeDto> badges = userBadgeRepository.findByUserId(user.getId())
                .stream()
                .map(ub -> new BadgeDto(ub.getBadge()))
                .collect(Collectors.toList());

        GamificationProfileDto dto = new GamificationProfileDto();
        dto.setPoints(score.getPoints());
        dto.setLevel(score.getLevel());
        dto.setTier(score.getTier().name());
        dto.setBadges(badges);
        dto.setNextLevelPoints(score.getLevel() * 100);

        return dto;
    }
}
