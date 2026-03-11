package com.example.financial.notification.service;

import com.example.financial.entity.AppUser;
import com.example.financial.notification.dto.NotificationDto;
import com.example.financial.notification.entity.Notification;
import com.example.financial.notification.entity.NotificationType;
import com.example.financial.notification.repository.NotificationRepository;
import com.example.financial.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final AppUserRepository appUserRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository,
                               AppUserRepository appUserRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.appUserRepository = appUserRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void sendNotification(String username, String message, NotificationType type) {
        AppUser user = appUserRepository.findFirstByEmailIgnoreCase(username)
                .orElse(null);

        if (user == null) {
            log.warn("Cannot send notification. User not found: {}", username);
            return;
        }

        Notification notification = new Notification(user, message, type);
        notificationRepository.save(notification);

        // Broadcast to specific user via WebSocket
        NotificationDto dto = new NotificationDto(notification);
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/notifications",
                dto
        );
        log.info("Sent WS notification to {}: {}", username, message);
    }

    public List<NotificationDto> getUserNotifications(AppUser user) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(NotificationDto::new)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(AppUser user) {
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public void markAsRead(UUID id) {
        Notification notification = notificationRepository.findById(id).orElse(null);
        if (notification != null) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(AppUser user) {
        notificationRepository.markAllAsReadByUserId(user.getId());
    }

    public void notifyPaymentSuccess(UUID userId, String message) {
        AppUser user = appUserRepository.findById(userId.toString()).orElse(null);
        if (user != null) {
            sendNotification(user.getEmail(), message, NotificationType.SUCCESS);
        }
    }

    public void notifyPaymentFailed(UUID userId, String message, String failureReason) {
        AppUser user = appUserRepository.findById(userId.toString()).orElse(null);
        if (user != null) {
            sendNotification(user.getEmail(), message + ". Reason: " + failureReason, NotificationType.ALERT);
        }
    }
}
