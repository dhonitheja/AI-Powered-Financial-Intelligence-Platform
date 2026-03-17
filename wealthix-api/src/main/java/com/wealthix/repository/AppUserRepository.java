package com.wealthix.repository;

import com.wealthix.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import java.util.UUID;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findFirstByEmailIgnoreCase(String email);

    Boolean existsByUsernameIgnoreCase(String username);

    Boolean existsByEmailIgnoreCase(String email);

    Optional<AppUser> findByStripeCustomerId(String stripeCustomerId);

    long countByLastLoginAtAfter(java.time.LocalDateTime cutoff);
}
