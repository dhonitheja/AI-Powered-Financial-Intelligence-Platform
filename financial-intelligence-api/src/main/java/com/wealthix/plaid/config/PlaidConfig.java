package com.wealthix.plaid.config;

import com.plaid.client.ApiClient;
import com.plaid.client.PlaidApi;
import com.plaid.client.model.*;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlaidConfig {

  @Value("${plaid.client-id}")
  private String clientId;

  @Value("${plaid.secret}")
  private String secret;

  @Value("${plaid.env}")
  private String environment;

  @Bean
  public PlaidApi plaidApi() {
    HashMap<String, String> apiKeys = new HashMap<>();
    apiKeys.put("clientId", clientId);
    apiKeys.put("secret", secret);
    apiKeys.put("plaidVersion", "2020-09-14");

    ApiClient apiClient = new ApiClient(apiKeys);
    
    // Select environment
    switch (environment.toLowerCase()) {
      case "production":
        apiClient.setPlaidAdapter(ApiClient.Production);
        break;
      case "development":
        apiClient.setPlaidAdapter(ApiClient.Development);
        break;
      default:
        apiClient.setPlaidAdapter(ApiClient.Sandbox);
    }
    return apiClient.createService(PlaidApi.class);
  }
}
