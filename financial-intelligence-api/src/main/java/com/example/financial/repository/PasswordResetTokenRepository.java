package com.example.financial.repository;

import com.example.financial.entity.PasswordResetToken;
import com.example.financial.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUser(AppUser user);
    void deleteByExpiryDateBefore(LocalDateTime now);
}
