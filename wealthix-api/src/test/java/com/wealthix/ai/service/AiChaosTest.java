package com.wealthix.ai.service;

import com.wealthix.ai.model.dto.ChatMessageDto;
import com.wealthix.ai.model.dto.ChatResponseDto;
import com.wealthix.ai.repository.ChatHistoryRepository;
import com.wealthix.ai.repository.SavingsGoalRepository;
import com.wealthix.autopay.repository.AutoPayScheduleRepository;
import com.wealthix.entity.AppUser;
import com.wealthix.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AiChaosTest {

    @Mock private AppUserRepository userRepository;
    @Mock private ChatHistoryRepository chatHistoryRepository;
    @Mock private SavingsGoalRepository savingsGoalRepository;
    @Mock private AutoPayScheduleRepository scheduleRepository;
    @Mock private AnomalyDetectionService anomalyDetectionService;
    @Mock private BudgetRecommendationService budgetRecommendationService;
    @Mock private SpendingForecastService spendingForecastService;
    @Mock private RestTemplate restTemplate;
    @Mock private com.wealthix.gamification.service.GamificationService gamificationService;

    @InjectMocks
    private AiAssistantService aiAssistantService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiAssistantService, "aiServiceUrl", "http://unreachable-ai");
        ReflectionTestUtils.setField(aiAssistantService, "internalSecret", "chaos-secret");
    }

    @Test
    void testChatServiceFailure_GracefulFallback() {
        // Arrange
        String username = "testuser@wealthix.com";
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail(username);

        ChatMessageDto req = new ChatMessageDto();
        req.setMessage("Is my spending safe?");
        req.setSessionId(UUID.randomUUID().toString());

        when(userRepository.findFirstByEmailIgnoreCase(username)).thenReturn(Optional.of(user));
        
        // Chaos stimulation: RestTemplate throws connection error
        when(restTemplate.postForEntity(any(), any(), eq(ChatResponseDto.class)))
                .thenThrow(new ResourceAccessException("Connection refused: AI service is down"));

        // Act
        ChatResponseDto fallbackResponse = aiAssistantService.chat(req, username);

        // Assert
        assertThat(fallbackResponse).isNotNull();
        assertThat(fallbackResponse.getReply()).contains("I'm having trouble connecting right now");
        assertThat(fallbackResponse.getSessionId()).isEqualTo(req.getSessionId());
    }

    @Test
    void testCircuitBreakerFallback_DirectCall() {
        // Arrange
        ChatMessageDto req = new ChatMessageDto();
        req.setSessionId("test-session");
        
        Throwable exception = new RuntimeException("AI Down");

        // Act (Simulate what AoP proxy would do)
        ChatResponseDto result = aiAssistantService.chatFallback(req, "testuser", exception);

        // Assert
        assertThat(result.getReply()).contains("The AI assistant is temporarily offline");
    }
}
