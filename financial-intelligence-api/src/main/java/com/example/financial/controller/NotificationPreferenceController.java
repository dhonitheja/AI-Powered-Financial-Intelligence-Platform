package com.example.financial.controller;

import com.example.financial.entity.NotificationPreference;
import com.example.financial.repository.NotificationPreferenceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/notifications/preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceRepository repository;

    public NotificationPreferenceController(NotificationPreferenceRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<NotificationPreference> getPreferences(@PathVariable String userId) {
        Optional<NotificationPreference> pref = repository.findByUserId(userId);
        return pref.map(ResponseEntity::ok)
                .orElseGet(() -> {
                    NotificationPreference defaultPref = new NotificationPreference();
                    defaultPref.setUserId(userId);
                    defaultPref.setEmailAddress("test@example.com");
                    return ResponseEntity.ok(repository.save(defaultPref));
                });
    }

    @PutMapping("/{userId}")
    public ResponseEntity<NotificationPreference> updatePreferences(@PathVariable String userId,
            @RequestBody NotificationPreference updatedPref) {
        NotificationPreference pref = repository.findByUserId(userId)
                .orElse(new NotificationPreference());

        pref.setUserId(userId);
        if (updatedPref.getEmailAddress() != null)
            pref.setEmailAddress(updatedPref.getEmailAddress());
        pref.setHighRiskAlerts(updatedPref.isHighRiskAlerts());
        pref.setSpendingAnomalies(updatedPref.isSpendingAnomalies());
        pref.setBudgetThresholds(updatedPref.isBudgetThresholds());
        pref.setBankSyncAlerts(updatedPref.isBankSyncAlerts());
        pref.setMonthlySummary(updatedPref.isMonthlySummary());

        return ResponseEntity.ok(repository.save(pref));
    }
}
