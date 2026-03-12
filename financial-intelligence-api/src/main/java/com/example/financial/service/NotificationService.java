package com.example.financial.service;

import com.example.financial.entity.NotificationPreference;
import com.example.financial.repository.NotificationPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service("emailNotificationService")
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final EmailService emailService;
    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationService(EmailService emailService, NotificationPreferenceRepository preferenceRepository) {
        this.emailService = emailService;
        this.preferenceRepository = preferenceRepository;
    }

    private NotificationPreference getOrCreatePreference(String userId) {
        Optional<NotificationPreference> optionalPref = preferenceRepository.findByUserId(userId);
        if (optionalPref.isPresent()) {
            return optionalPref.get();
        }

        NotificationPreference newPref = new NotificationPreference();
        newPref.setUserId(userId);
        newPref.setEmailAddress("test@example.com"); // Fallback for testing
        return preferenceRepository.save(newPref);
    }

    public void sendHighRiskAlert(String userId, String transactionName, Double amount, String reason) {
        NotificationPreference prefs = getOrCreatePreference(userId);
        if (!prefs.isHighRiskAlerts()) {
            return;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("title", "High Risk Transaction Alert");
        model.put("transactionName", transactionName);
        model.put("amount", amount);
        model.put("reason", reason);

        emailService.sendHtmlEmail(prefs.getEmailAddress(), "Security Alert: High Risk Transaction Detected",
                "high-risk-alert", model);
    }

    public void sendSpendingAnomalyAlert(String userId, String category, Double amount, String recommendation) {
        NotificationPreference prefs = getOrCreatePreference(userId);
        if (!prefs.isSpendingAnomalies()) {
            return;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Spending Anomaly Detected");
        model.put("category", category);
        model.put("amount", amount);
        model.put("recommendation", recommendation);

        emailService.sendHtmlEmail(prefs.getEmailAddress(), "Insight: Unusual Spending Pattern", "spending-anomaly",
                model);
    }

    public void sendBudgetThresholdExceeded(String userId, String category, Double limit, Double currentSpend) {
        NotificationPreference prefs = getOrCreatePreference(userId);
        if (!prefs.isBudgetThresholds()) {
            return;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Budget Limit Exceeded");
        model.put("category", category);
        model.put("limit", limit);
        model.put("currentSpend", currentSpend);

        emailService.sendHtmlEmail(prefs.getEmailAddress(), "Budget Alert: Limit Exceeded", "budget-threshold", model);
    }

    public void sendBankSyncCompletion(String userId, int newTransactionsCount) {
        NotificationPreference prefs = getOrCreatePreference(userId);
        if (!prefs.isBankSyncAlerts() || newTransactionsCount == 0) {
            return;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Bank Sync Completed");
        model.put("newTransactionsCount", newTransactionsCount);

        emailService.sendHtmlEmail(prefs.getEmailAddress(), "Update: Bank Sync Complete", "bank-sync", model);
    }

    public void sendMonthlySummary(String userId, String htmlSummary) {
        NotificationPreference prefs = getOrCreatePreference(userId);
        if (!prefs.isMonthlySummary()) {
            return;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Your Monthly AI Financial Summary");
        model.put("summaryContent", htmlSummary);

        emailService.sendHtmlEmail(prefs.getEmailAddress(), "Your Monthly Financial Report", "monthly-summary", model);
    }
}
