package com.example.financial.config;

import com.plaid.client.ApiClient;
import com.plaid.client.request.PlaidApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class PlaidConfig {
    private static final Logger log = LoggerFactory.getLogger(PlaidConfig.class);

    @Value("${plaid.client-id:}")
    private String plaidClientId;

    @Value("${plaid.secret:}")
    private String plaidSecret;

    @Value("${plaid.env:sandbox}")
    private String plaidEnv;

    @Bean
    public PlaidApi plaidApi() {
        Map<String, String> apiKeys = new HashMap<>();
        apiKeys.put("clientId", plaidClientId);
        apiKeys.put("secret", plaidSecret);

        ApiClient apiClient = new ApiClient(apiKeys);

        if ("production".equalsIgnoreCase(plaidEnv)) {
            apiClient.setPlaidAdapter(ApiClient.Production);
        } else {
            // Default to Sandbox for both 'sandbox' and 'development' if Development
            // constant is missing
            apiClient.setPlaidAdapter(ApiClient.Sandbox);
        }

        return apiClient.createService(PlaidApi.class);
    }
}
