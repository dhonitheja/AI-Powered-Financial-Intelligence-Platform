package com.wealthix.gamification.controller;

import com.wealthix.dto.ApiResponse;
import com.wealthix.entity.AppUser;
import com.wealthix.gamification.dto.GamificationProfileDto;
import com.wealthix.gamification.service.GamificationService;
import com.wealthix.repository.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gamification")
public class GamificationController {

    private final GamificationService gamificationService;
    private final AppUserRepository userRepository;

    public GamificationController(GamificationService gamificationService,
                                  AppUserRepository userRepository) {
        this.gamificationService = gamificationService;
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<GamificationProfileDto>> getProfile(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof com.wealthix.security.UserDetailsImpl principal)) {
            return ResponseEntity.status(401).build();
        }

        AppUser user = userRepository.findById(UUID.fromString(principal.getId())).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        GamificationProfileDto profile = gamificationService.getProfile(user);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
