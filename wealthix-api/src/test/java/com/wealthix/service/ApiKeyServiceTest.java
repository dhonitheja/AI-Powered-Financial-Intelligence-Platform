package com.wealthix.service;

import com.wealthix.entity.ApiKey;
import com.wealthix.repository.ApiKeyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock ApiKeyRepository apiKeyRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks ApiKeyService apiKeyService;

    @Test
    void generateKey_returnsHashedValue() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_key");
        
        UUID userId = UUID.randomUUID();
        String rawKey = apiKeyService.generateKey(userId, "Test Key");

        assertThat(rawKey).startsWith("wx_");
        
        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getApiKeyHashed()).isEqualTo("hashed_key");
    }

    @Test
    void validateKey_hashesInput_andCompares() {
        ApiKey apiKey = ApiKey.builder()
                .apiKeyHashed("hashed_key")
                .active(true)
                .deleted(false)
                .build();
        
        when(apiKeyRepository.findAll()).thenReturn(java.util.List.of(apiKey));
        when(passwordEncoder.matches("wx_raw", "hashed_key")).thenReturn(true);

        boolean valid = apiKeyService.validateKey("wx_raw");

        assertThat(valid).isTrue();
        verify(passwordEncoder).matches("wx_raw", "hashed_key");
    }

    @Test
    void revokeKey_setsDeletedTrue() {
        UUID keyId = UUID.randomUUID();
        ApiKey apiKey = new ApiKey();
        apiKey.setId(keyId);
        apiKey.setActive(true);
        apiKey.setDeleted(false);

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKey));

        apiKeyService.revokeKey(keyId);

        assertThat(apiKey.isDeleted()).isTrue();
        assertThat(apiKey.isActive()).isFalse();
        verify(apiKeyRepository).save(apiKey);
    }
}
