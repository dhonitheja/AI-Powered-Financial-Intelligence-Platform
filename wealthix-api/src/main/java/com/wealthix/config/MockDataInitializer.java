package com.wealthix.config;

import com.wealthix.autopay.model.entity.AutoPaySchedule;
import com.wealthix.autopay.model.entity.PaymentCategory;
import com.wealthix.autopay.model.entity.PaymentFrequency;
import com.wealthix.autopay.repository.AutoPayScheduleRepository;
import com.wealthix.entity.AppUser;
import com.wealthix.entity.UserBankConnection;
import com.wealthix.repository.AppUserRepository;
import com.wealthix.repository.UserBankConnectionRepository;
import com.wealthix.service.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ensures the database has 1 stable mock item for testing webhooks.
 * This runs ONLY in 'dev' or 'qa' profiles (uncomment the profile as needed).
 */
@Configuration
public class MockDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(MockDataInitializer.class);
    
    // THE STABLE TEST ITEM ID - use this in trigger_plaid_webhook.py
    public static final String MOCK_ITEM_ID = "item_QA_TEST_001";
    public static final String MOCK_USER_EMAIL = "qa-tester@wealthix.com";

    @Bean
    @Profile("!prod") // Don't run this in production!
    public CommandLineRunner initMockPlaidData(
            AppUserRepository userRepository,
            UserBankConnectionRepository connectionRepository,
            AutoPayScheduleRepository autoPayRepository,
            EncryptionService encryptionService,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        
        return args -> {
            log.info("[QA] Initializing stable mock test data...");

            // 1. Ensure QA User exists and has the correct HASHED password
            AppUser user = userRepository.findFirstByEmailIgnoreCase(MOCK_USER_EMAIL)
                    .orElseGet(() -> {
                        log.info("[QA] Creating QA user: {}", MOCK_USER_EMAIL);
                        AppUser newUser = new AppUser();
                        newUser.setEmail(MOCK_USER_EMAIL);
                        newUser.setUsername("qa_tester");
                        return newUser;
                    });
            
            // Force reset password to encoded 'qa_secure_dev_pass' for every dev startup
            user.setPassword(passwordEncoder.encode("qa_secure_dev_pass"));
            user = userRepository.save(user);

            // 2. Ensure Mock Connection exists for this user
            if (connectionRepository.findByItemId(MOCK_ITEM_ID).isEmpty()) {
                log.info("[QA] Creating mock bank connection: item_id={}", MOCK_ITEM_ID);
                UserBankConnection connection = new UserBankConnection();
                connection.setUserId(user.getId()); // Store UUID directly
                connection.setItemId(MOCK_ITEM_ID);
                connection.setInstitutionId("ins_123"); 
                connection.setInstitutionName("Senior QA Mock Bank");
                connection.setEncryptedAccessToken(encryptionService.encrypt("mock_qa_access_token")); 
                connection.setActive(true);
                connection.setUpdatedAt(LocalDateTime.now());
                connectionRepository.save(connection);
            }

            // 3. Ensure EMI/Loan Mock Data exists
            if (autoPayRepository.countByUserIdAndActiveTrue(user.getId()) == 0) {
                log.info("[QA] Creating mock EMI/Loan schedules...");

                // Home Loan
                AutoPaySchedule homeLoan = new AutoPaySchedule();
                homeLoan.setUser(user);
                homeLoan.setPaymentName("Chase Home Mortgage");
                homeLoan.setPaymentCategory(PaymentCategory.HOME_LOAN);
                homeLoan.setPaymentProvider("JPMorgan Chase");
                homeLoan.setAmount(new BigDecimal("2850.45"));
                homeLoan.setFrequency(PaymentFrequency.MONTHLY);
                homeLoan.setNextDueDate(LocalDate.now().plusDays(12));
                homeLoan.setDueDayOfMonth(LocalDate.now().plusDays(12).getDayOfMonth());
                homeLoan.setAutoExecute(true);
                autoPayRepository.save(homeLoan);

                // Auto Loan
                AutoPaySchedule autoLoan = new AutoPaySchedule();
                autoLoan.setUser(user);
                autoLoan.setPaymentName("Tesla Finance Auto Loan");
                autoLoan.setPaymentCategory(PaymentCategory.AUTO_LOAN);
                autoLoan.setPaymentProvider("Tesla Financial Services");
                autoLoan.setAmount(new BigDecimal("842.12"));
                autoLoan.setFrequency(PaymentFrequency.MONTHLY);
                autoLoan.setNextDueDate(LocalDate.now().plusDays(5));
                autoLoan.setDueDayOfMonth(LocalDate.now().plusDays(5).getDayOfMonth());
                autoLoan.setAutoExecute(false);
                autoPayRepository.save(autoLoan);

                // Personal Loan (Overdue example)
                AutoPaySchedule personalLoan = new AutoPaySchedule();
                personalLoan.setUser(user);
                personalLoan.setPaymentName("SoFi Personal Loan");
                personalLoan.setPaymentCategory(PaymentCategory.PERSONAL_LOAN);
                personalLoan.setPaymentProvider("SoFi Bank");
                personalLoan.setAmount(new BigDecimal("420.00"));
                personalLoan.setFrequency(PaymentFrequency.MONTHLY);
                personalLoan.setNextDueDate(LocalDate.now().minusDays(3)); // OVERDUE
                personalLoan.setDueDayOfMonth(LocalDate.now().minusDays(3).getDayOfMonth());
                personalLoan.setAutoExecute(true);
                autoPayRepository.save(personalLoan);

                log.info("[QA] Mock EMI data created.");
            }
        };
    }
}
