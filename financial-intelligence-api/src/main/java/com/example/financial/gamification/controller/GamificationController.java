package com.example.financial.gamification.controller;

import com.example.financial.dto.ApiResponse;
import com.example.financial.entity.AppUser;
import com.example.financial.gamification.dto.GamificationProfileDto;
import com.example.financial.gamification.service.GamificationService;
import com.example.financial.repository.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        if (auth == null) return ResponseEntity.status(401).build();

        AppUser user = userRepository.findFirstByEmailIgnoreCase(auth.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        GamificationProfileDto profile = gamificationService.getProfile(user);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
