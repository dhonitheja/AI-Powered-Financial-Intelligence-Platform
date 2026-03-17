package com.wealthix.config;

import com.wealthix.entity.AppUser;
import com.wealthix.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSeeder(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("[Seeder] No users found. Seeding default demo user...");
            
            AppUser demoUser = new AppUser(
                "demo_user",
                "dhonitheja007@gmail.com",
                passwordEncoder.encode("password123")
            );
            
            userRepository.save(demoUser);
            log.info("[Seeder] Default user 'dhonitheja007@gmail.com' created with password 'password123'");
        } else {
            log.info("[Seeder] Database already contains users. Skipping seeding.");
        }
    }
}
