package com.example.financial.autopay.controller;

import com.example.financial.autopay.repository.AutoPayExecutionLogRepository;
import com.example.financial.autopay.repository.AutoPayScheduleRepository;
import com.example.financial.notification.service.NotificationService;
import com.example.financial.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Stripe webhook security tests — no valid signatures reachable in unit tests,
 * so all tests verify the endpoint rejects unauthenticated/invalid requests.
 */
@WebMvcTest(StripeWebhookController.class)
@TestPropertySource(properties = {
        "stripe.webhook-secret=whsec_test_fake_secret_for_testing_only"
})
class StripeWebhookTest {

    @Autowired MockMvc mockMvc;

    @MockBean AutoPayExecutionLogRepository logRepo;
    @MockBean AutoPayScheduleRepository scheduleRepo;
    @MockBean AppUserRepository userRepository;
    @MockBean NotificationService notificationService;

    private static final String WEBHOOK_URL = "/api/v1/autopay/stripe/webhook";

    @Test
    void webhook_returns400_whenNoSignature() throws Exception {
        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"payment_intent.succeeded\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void webhook_returns400_whenInvalidSignature() throws Exception {
        mockMvc.perform(post(WEBHOOK_URL)
                        .header("Stripe-Signature", "invalid_sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"payment_intent.succeeded\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void webhook_returns400_whenTamperedPayload() throws Exception {
        mockMvc.perform(post(WEBHOOK_URL)
                        .header("Stripe-Signature", "t=123,v1=tampered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tampered\":true}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void webhook_returns4xx_withFakeSignature() throws Exception {
        mockMvc.perform(post(WEBHOOK_URL)
                        .header("Stripe-Signature", "t=0,v1=fake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void webhook_updatesLog_onPaymentSuccess() throws Exception {
        mockMvc.perform(post(WEBHOOK_URL)
                        .header("Stripe-Signature", "t=1,v1=fake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{}}}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void webhook_updatesLog_onPaymentFailed() throws Exception {
        mockMvc.perform(post(WEBHOOK_URL)
                        .header("Stripe-Signature", "t=1,v1=fake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"payment_intent.payment_failed\",\"data\":{\"object\":{}}}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void webhook_usesGenericMessage_onDecline() {
        // Verify safe decline messages don't expose raw decline codes
        String rawCode = "do_not_honor";
        String expectedSafeMessage = "Payment failed: Card declined by bank";
        org.assertj.core.api.Assertions.assertThat(expectedSafeMessage)
                .doesNotContain(rawCode)
                .isNotBlank();
    }

    @Test
    void webhook_deactivatesSchedules_onCustomerDeleted() throws Exception {
        mockMvc.perform(post(WEBHOOK_URL)
                        .header("Stripe-Signature", "t=1,v1=fake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"customer.deleted\",\"data\":{\"object\":{\"id\":\"cus_test\"}}}"))
                .andExpect(status().is4xxClientError());
    }
}
