package com.wealthix.ai.controller;

import com.wealthix.ai.model.entity.WealthInsight;
import com.wealthix.ai.repository.WealthInsightRepository;
import com.wealthix.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class WealthInsightController {

    private final WealthInsightRepository insightRepo;

    @GetMapping("/latest-insight")
    public ResponseEntity<WealthInsight> getLatestInsight(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return insightRepo.findTopByUserIdOrderByCreatedAtDesc(UUID.fromString(userDetails.getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
