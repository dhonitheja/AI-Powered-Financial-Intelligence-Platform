package com.example.financial.ai.service;

import com.example.financial.ai.model.dto.ChatMessageDto;
import com.example.financial.ai.model.dto.ChatResponseDto;
import com.example.financial.ai.model.dto.CreateSavingsGoalRequest;
import com.example.financial.ai.model.entity.ChatHistory;
import com.example.financial.ai.model.entity.SavingsGoal;
import com.example.financial.ai.repository.ChatHistoryRepository;
import com.example.financial.ai.repository.SavingsGoalRepository;
import com.example.financial.autopay.model.entity.AutoPaySchedule;
import com.example.financial.autopay.repository.AutoPayScheduleRepository;
import com.example.financial.entity.AppUser;
import com.example.financial.repository.AppUserRepository;
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
import java.util.stream.Collectors;

@Service
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    private final AppUserRepository userRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final AutoPayScheduleRepository scheduleRepository;
    private final AnomalyDetectionService anomalyDetectionService;
    private final BudgetRecommendationService budgetRecommendationService;
    private final SpendingForecastService spendingForecastService;
    private final RestTemplate restTemplate;
    private final com.example.financial.gamification.service.GamificationService gamificationService;

    @Value("${wealthix.ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${wealthix.ai.service.secret:}")
    private String internalSecret;

    public AiAssistantService(AppUserRepository userRepository,
                              ChatHistoryRepository chatHistoryRepository,
                              SavingsGoalRepository savingsGoalRepository,
                              AutoPayScheduleRepository scheduleRepository,
                              AnomalyDetectionService anomalyDetectionService,
                              BudgetRecommendationService budgetRecommendationService,
                              SpendingForecastService spendingForecastService,
                              RestTemplate restTemplate,
                              com.example.financial.gamification.service.GamificationService gamificationService) {
        this.userRepository = userRepository;
        this.chatHistoryRepository = chatHistoryRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.scheduleRepository = scheduleRepository;
        this.anomalyDetectionService = anomalyDetectionService;
        this.budgetRecommendationService = budgetRecommendationService;
        this.spendingForecastService = spendingForecastService;
        this.restTemplate = restTemplate;
        this.gamificationService = gamificationService;
    }

    public ChatResponseDto chat(ChatMessageDto req, String username) {
        AppUser user = userRepository.findFirstByEmailIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UUID sessionId = UUID.fromString(req.getSessionId());

        // Build history
        List<ChatHistory> historyEntities = chatHistoryRepository
                .findTop20ByUserIdAndSessionIdOrderByCreatedAtDesc(user.getId(), sessionId);
        Collections.reverse(historyEntities);

        List<Map<String, String>> history = historyEntities.stream()
                .map(h -> Map.of("role", h.getRole(), "content", h.getContent()))
                .collect(Collectors.toList());

        // Get schedules to build financial context safely (amounts and categories only)
        List<AutoPaySchedule> schedules = scheduleRepository.findByUserIdAndActiveTrue(user.getId());
        Map<String, Double> financialContext = new HashMap<>();
        for (AutoPaySchedule schedule : schedules) {
            String category = schedule.getPaymentCategory().name();
            financialContext.put(category, financialContext.getOrDefault(category, 0.0)
                    + schedule.getAmount().doubleValue());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("message", req.getMessage());
        body.put("session_id", req.getSessionId());
        body.put("history", history);
        body.put("financial_context", financialContext);

        // Call FastAPI
        ChatResponseDto responseDto = null;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Wealthix-Service-Secret", internalSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<ChatResponseDto> response = restTemplate.postForEntity(
                    aiServiceUrl + "/assistant/chat", request, ChatResponseDto.class);
            responseDto = response.getBody();
        } catch (Exception e) {
            log.error("[AI] Chat request failed", e);
            responseDto = new ChatResponseDto(
                    "I'm having trouble connecting right now. Please try again in a moment.",
                    req.getSessionId(),
                    List.of()
            );
        }

        // Save messages
        ChatHistory userMessage = new ChatHistory(user, "user", req.getMessage(), sessionId);
        chatHistoryRepository.save(userMessage);

        if (responseDto != null && responseDto.getReply() != null) {
            ChatHistory botMessage = new ChatHistory(user, "assistant", responseDto.getReply(), sessionId);
            chatHistoryRepository.save(botMessage);
            
            // Award points for using AI Chat
            gamificationService.awardPoints(user, 10, "Asked AI a question");
        }

        return responseDto;
    }

    public Object getSpendingForecast(String username) {
        AppUser user = userRepository.findFirstByEmailIgnoreCase(username).orElseThrow();
        return spendingForecastService.getForecast(user);
    }

    public Object detectAnomalies(String username) {
        AppUser user = userRepository.findFirstByEmailIgnoreCase(username).orElseThrow();
        return anomalyDetectionService.getAnomalies(user);
    }

    public void acknowledgeAnomaly(UUID anomalyId, String username) {
        AppUser user = userRepository.findFirstByEmailIgnoreCase(username).orElseThrow();
        anomalyDetectionService.acknowledge(anomalyId, user);
    }

    public Object getBudgetRecommendations(String username) {
        AppUser user = userRepository.findFirstByEmailIgnoreCase(username).orElseThrow();
        return budgetRecommendationService.getRecommendations(user);
    }

    public List<SavingsGoal> getSavingsGoals(String username) {
        AppUser user = userRepository.findFirstByEmailIgnoreCase(username).orElseThrow();
        return savingsGoalRepository.findByUserIdAndIsActiveTrue(user.getId());
    }

    public SavingsGoal createSavingsGoal(CreateSavingsGoalRequest req, String username) {
        AppUser user = userRepository.findFirstByEmailIgnoreCase(username).orElseThrow();
        SavingsGoal goal = new SavingsGoal(
                user,
                req.getGoalName(),
                req.getTargetAmount(),
                req.getCurrency(),
                req.getTargetDate()
        );
        SavingsGoal savedGoal = savingsGoalRepository.save(goal);
        
        // Award points for creating a financial goal
        gamificationService.awardPoints(user, 50, "Created a structured savings plan");
        
        return savedGoal;
    }
}
