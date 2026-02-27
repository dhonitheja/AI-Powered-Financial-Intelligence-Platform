package com.example.financial.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${spring.ai.service-url:http://127.0.0.1:8090}")
    private String aiServiceUrl;

    @Bean
    public WebClient aiWebClient(WebClient.Builder builder) {
        return builder.baseUrl(aiServiceUrl).build();
    }
}
