package com.wealthix.repository;

import com.wealthix.entity.PasswordResetToken;
import com.wealthix.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUser(AppUser user);
    void deleteByExpiryDateBefore(LocalDateTime now);
}
