package com.example.financial.notification.controller;

import com.example.financial.dto.ApiResponse;
import com.example.financial.entity.AppUser;
import com.example.financial.notification.dto.NotificationDto;
import com.example.financial.notification.service.NotificationService;
import com.example.financial.repository.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AppUserRepository userRepository;

    public NotificationController(NotificationService notificationService,
                                  AppUserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotifications(
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        AppUser user = userRepository.findFirstByEmailIgnoreCase(auth.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        List<NotificationDto> dtos = notificationService.getUserNotifications(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", dtos.stream().limit(size).toList());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        AppUser user = userRepository.findFirstByEmailIgnoreCase(auth.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        long count = notificationService.getUnreadCount(user);
        Map<String, Long> data = new HashMap<>();
        data.put("count", count);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable UUID id, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<String>> markAllAsRead(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        AppUser user = userRepository.findFirstByEmailIgnoreCase(auth.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        notificationService.markAllAsRead(user);
        return ResponseEntity.ok(ApiResponse.success("Notifications marked as read"));
    }
}
