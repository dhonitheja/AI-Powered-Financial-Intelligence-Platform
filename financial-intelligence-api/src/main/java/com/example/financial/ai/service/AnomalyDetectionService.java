package com.example.financial.ai.service;

import com.example.financial.ai.model.entity.SpendingAnomaly;
import com.example.financial.ai.repository.SpendingAnomalyRepository;
import com.example.financial.autopay.model.entity.AutoPaySchedule;
import com.example.financial.autopay.repository.AutoPayScheduleRepository;
import com.example.financial.entity.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);

    private final AutoPayScheduleRepository scheduleRepository;
    private final SpendingAnomalyRepository anomalyRepository;
    private final RestTemplate restTemplate;

    @Value("${wealthix.ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${wealthix.ai.service.secret:}")
    private String internalSecret;

    public AnomalyDetectionService(AutoPayScheduleRepository scheduleRepository,
                                   SpendingAnomalyRepository anomalyRepository,
                                   RestTemplate restTemplate) {
        this.scheduleRepository = scheduleRepository;
        this.anomalyRepository = anomalyRepository;
        this.restTemplate = restTemplate;
    }

    public Object getAnomalies(AppUser user) {
        // Prepare request
        List<AutoPaySchedule> schedules = scheduleRepository.findByUserIdAndActiveTrue(user.getId());

        List<Map<String, Object>> scheduleList = new ArrayList<>();
        for (AutoPaySchedule s : schedules) {
            Map<String, Object> map = new HashMap<>();
            map.put("category", s.getPaymentCategory().name());
            map.put("amount", s.getAmount());
            scheduleList.add(map);
        }

        // Just an empty context for historical averages right now,
        // in a real app this would query execution logs
        Map<String, Object> body = new HashMap<>();
        body.put("schedules", scheduleList);
        body.put("historical_averages", new HashMap<>());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Wealthix-Service-Secret", internalSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Object> response = restTemplate.postForEntity(
                    aiServiceUrl + "/anomaly/detect", request, Object.class);

            // Typically you'd save DB records here, but the python side
            // just returns transient anomaly reports.
            return response.getBody();
        } catch (Exception e) {
            log.error("[AI] Anomaly detection failed", e);
            return Map.of("anomalies", List.of(), "risk_level", "LOW");
        }
    }

    public void acknowledge(UUID anomalyId, AppUser user) {
        SpendingAnomaly anomaly = anomalyRepository.findById(anomalyId).orElseThrow();
        if (!anomaly.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        anomaly.setIsAcknowledged(true);
        anomaly.setAcknowledgedAt(java.time.LocalDateTime.now());
        anomalyRepository.save(anomaly);
    }
}
