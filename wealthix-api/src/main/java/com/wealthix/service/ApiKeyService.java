package com.wealthix.service;

import com.wealthix.entity.ApiKey;
import com.wealthix.repository.ApiKeyRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository, PasswordEncoder passwordEncoder) {
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String generateKey(UUID userId, String name) {
        byte[] keyBytes = new byte[32];
        secureRandom.nextBytes(keyBytes);
        String rawKey = "wx_" + Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);

        ApiKey apiKey = ApiKey.builder()
                .userId(userId)
                .name(name)
                .apiKeyHashed(passwordEncoder.encode(rawKey))
                .active(true)
                .deleted(false)
                .build();

        apiKeyRepository.save(apiKey);
        return rawKey;
    }

    public boolean validateKey(String rawKey) {
        // This is tricky because BCrypt is not searchable. 
        // In a real system, we'd use a prefix or a DB search with a fast hash for lookup.
        // For simplicity here, we'll assume there's a way to find it or we check all active keys (bad for perf).
        // Let's assume we use a lookup hash or similar.
        // Instead, I'll use a more standard "API key" pattern: key = access_id + secret_key.
        // But the prompt says "hashes input and compares".
        
        return apiKeyRepository.findAll().stream()
                .filter(k -> !k.isDeleted() && k.isActive())
                .anyMatch(k -> passwordEncoder.matches(rawKey, k.getApiKeyHashed()));
    }

    @Transactional
    public void revokeKey(UUID keyId) {
        apiKeyRepository.findById(keyId).ifPresent(k -> {
            k.setDeleted(true);
            k.setActive(false);
            apiKeyRepository.save(k);
        });
    }
}
