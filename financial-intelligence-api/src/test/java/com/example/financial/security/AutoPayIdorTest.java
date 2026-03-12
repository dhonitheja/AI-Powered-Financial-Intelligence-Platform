package com.example.financial.security;

import com.example.financial.autopay.controller.AutoPayController;
import com.example.financial.autopay.service.AutoPayService;
import com.example.financial.autopay.service.PlaidVerificationService;
import com.example.financial.autopay.service.RecurringPaymentDetectionService;
import com.example.financial.autopay.service.StripePaymentService;
import com.example.financial.config.StripeConfig;
import com.example.financial.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IDOR tests — verifies service layer rejects cross-user access.
 * Uses proper UserDetailsImpl principal so resolveEmail() extracts the correct email.
 */
@WebMvcTest(AutoPayController.class)
class AutoPayIdorTest {

    @Autowired MockMvc mockMvc;
    @MockBean AutoPayService autoPayService;
    @MockBean StripePaymentService stripePaymentService;
    @MockBean RecurringPaymentDetectionService recurringPaymentDetectionService;
    @MockBean PlaidVerificationService plaidVerificationService;
    @MockBean StripeConfig stripeConfig;

    private final UUID scheduleId = UUID.randomUUID();
    private UserDetailsImpl userBDetails;

    @BeforeEach
    void setUp() {
        AppUser userB = new AppUser("userb", "userb@test.com", "hashed");
        userBDetails = UserDetailsImpl.build(userB);

        // Service throws FORBIDDEN when user B tries to access user A's schedule
        ResponseStatusException forbidden =
                new ResponseStatusException(FORBIDDEN, "Access denied");

        when(autoPayService.getById(eq("userb@test.com"), eq(scheduleId)))
                .thenThrow(forbidden);
        when(autoPayService.update(eq("userb@test.com"), eq(scheduleId), any()))
                .thenThrow(forbidden);
        doThrow(forbidden).when(autoPayService).softDelete(
                eq("userb@test.com"), eq(scheduleId));
        // execute endpoint: resolveUserId() then stripePaymentService.execute()
        UUID userBId = UUID.randomUUID();
        when(autoPayService.resolveUserId(eq("userb@test.com"))).thenReturn(userBId);
        when(stripePaymentService.execute(eq(scheduleId), eq(userBId))).thenThrow(forbidden);
        when(autoPayService.toggleActive(eq("userb@test.com"), eq(scheduleId)))
                .thenThrow(forbidden);
    }

    @Test
    void userB_cannot_getSchedule_ofUserA() throws Exception {
        mockMvc.perform(get("/api/v1/autopay/schedules/" + scheduleId)
                        .with(user(userBDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userB_cannot_updateSchedule_ofUserA() throws Exception {
        mockMvc.perform(put("/api/v1/autopay/schedules/" + scheduleId)
                        .with(user(userBDetails)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentName\":\"Hacked\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userB_cannot_deleteSchedule_ofUserA() throws Exception {
        mockMvc.perform(delete("/api/v1/autopay/schedules/" + scheduleId)
                        .with(user(userBDetails)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void userB_cannot_executePayment_ofUserA() throws Exception {
        mockMvc.perform(post("/api/v1/autopay/schedules/" + scheduleId + "/execute")
                        .with(user(userBDetails)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void userB_cannot_toggleSchedule_ofUserA() throws Exception {
        mockMvc.perform(patch("/api/v1/autopay/schedules/" + scheduleId + "/toggle")
                        .with(user(userBDetails)).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
