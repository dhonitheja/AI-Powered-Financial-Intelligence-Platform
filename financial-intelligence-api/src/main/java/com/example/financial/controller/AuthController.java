package com.example.financial.controller;

import com.example.financial.dto.LoginRequest;
import com.example.financial.dto.MessageResponse;
import com.example.financial.dto.SignupRequest;
import com.example.financial.dto.UserInfoResponse;
import com.example.financial.dto.TwoFactorSetupResponse;
import com.example.financial.dto.VerifyTwoFactorRequest;
import com.example.financial.entity.AppUser;
import com.example.financial.repository.AppUserRepository;
import com.example.financial.security.JwtUtils;
import com.example.financial.security.LoginRateLimiterService;
import com.example.financial.security.TotpAttemptService;
import com.example.financial.security.TwoFactorAuthService;
import com.example.financial.security.UserDetailsImpl;
import com.example.financial.dto.ForgotPasswordRequest;
import com.example.financial.dto.ResetPasswordRequest;
import com.example.financial.entity.PasswordResetToken;
import com.example.financial.repository.PasswordResetTokenRepository;
import com.example.financial.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final TwoFactorAuthService tfaService;
    private final LoginRateLimiterService loginRateLimiter;
    private final TotpAttemptService totpAttemptService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

    public AuthController(
            AuthenticationManager authenticationManager,
            AppUserRepository userRepository,
            PasswordEncoder encoder,
            JwtUtils jwtUtils,
            TwoFactorAuthService tfaService,
            LoginRateLimiterService loginRateLimiter,
            TotpAttemptService totpAttemptService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.tfaService = tfaService;
        this.loginRateLimiter = loginRateLimiter;
        this.totpAttemptService = totpAttemptService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
    }

    // ─── Login ─────────────────────────────────────────────────────────────────

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {

        if (loginRequest == null
                || loginRequest.getEmail() == null
                || loginRequest.getPassword() == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Email and password must be provided"));
        }

        String clientIp = getClientIp(request);

        // ── Rate limit check ──────────────────────────────────────────────────
        if (loginRateLimiter.isBlocked(clientIp)) {
            log.warn("[Auth] Login blocked for IP {} – rate limit exceeded", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse(
                            "Too many login attempts. Please wait 1 minute and try again."));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            AppUser user = userRepository.findFirstByEmailIgnoreCase(loginRequest.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

            // ── Success: reset rate limiter ───────────────────────────────────
            loginRateLimiter.resetAttempts(clientIp);

            // ── 2FA gate ──────────────────────────────────────────────────────
            if (user.isTwoFactorEnabled()) {
                log.info("[Auth] 2FA required for user {}", user.getEmail());
                return ResponseEntity.accepted() // HTTP 202
                        .body(Map.of(
                                "twoFactorRequired", true,
                                "email", user.getEmail()));
            }

            // ── Issue JWT cookie ──────────────────────────────────────────────
            ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);
            log.info("[Auth] Login successful for {}", user.getEmail());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(new UserInfoResponse(
                            userDetails.getId(),
                            userDetails.getUsername(),
                            userDetails.getEmail(),
                            user.isTwoFactorEnabled()));

        } catch (BadCredentialsException e) {
            loginRateLimiter.recordFailedAttempt(clientIp);
            int remaining = Math.max(0, 5 - loginRateLimiter.getAttemptCount(clientIp));
            log.warn("[Auth] Bad credentials from IP {} – {} attempts remaining", clientIp, remaining);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse(
                            "Invalid email or password. " + remaining + " attempts remaining."));

        } catch (UsernameNotFoundException e) {
            loginRateLimiter.recordFailedAttempt(clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Invalid email or password."));

        } catch (Exception e) {
            log.error("[Auth] Unexpected error during signin: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Authentication failed. Please try again."));
        }
    }

    // ─── Register ──────────────────────────────────────────────────────────────

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsernameIgnoreCase(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }
        if (userRepository.existsByEmailIgnoreCase(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        AppUser user = new AppUser(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    // ─── Forgot/Reset Password ─────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Email is required"));
        }

        AppUser user = userRepository.findFirstByEmailIgnoreCase(request.getEmail()).orElse(null);
        if (user == null) {
            // Return success even if not found to prevent email enumeration
            return ResponseEntity.ok(new MessageResponse("If your email is registered, you will receive a reset link shortly."));
        }

        // Generate token and save
        String tokenStr = java.util.UUID.randomUUID().toString();
        
        // Remove old tokens
        passwordResetTokenRepository.findByUser(user).ifPresent(passwordResetTokenRepository::delete);
        
        PasswordResetToken resetToken = new PasswordResetToken(tokenStr, user);
        passwordResetTokenRepository.save(resetToken);

        // Send email
        String resetLink = frontendUrl + "/reset-password?token=" + tokenStr;
        String content = "Hello " + user.getUsername() + ",\n\n"
                + "You requested to reset your password. Please click the link below to set a new password:\n"
                + resetLink + "\n\n"
                + "This link will expire in 24 hours.\n\n"
                + "If you did not request this, please ignore this email.";

        emailService.sendPlainTextEmail(user.getEmail(), "Password Reset Request", content);

        return ResponseEntity.ok(new MessageResponse("If your email is registered, you will receive a reset link shortly."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (request.getToken() == null || request.getNewPassword() == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Token and new password required"));
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken()).orElse(null);
        if (resetToken == null || resetToken.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid or expired reset token"));
        }

        AppUser user = resetToken.getUser();
        user.setPassword(encoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);

        return ResponseEntity.ok(new MessageResponse("Password has been reset successfully. You can now log in."));
    }


    // ─── Logout ────────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new MessageResponse("You've been signed out!"));
    }

    // ─── Session Validation ────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponse("Not authenticated"));
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            AppUser user = userRepository.findFirstByEmailIgnoreCase(userDetails.getEmail()).orElse(null);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponse("User account not found"));
            }

            return ResponseEntity.ok(new UserInfoResponse(
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    user.isTwoFactorEnabled()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Session validation failed"));
        }
    }

    // ─── 2FA Setup ─────────────────────────────────────────────────────────────

    @GetMapping("/2fa/setup")
    public ResponseEntity<?> setup2FA() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponse("Not authenticated"));
            }

            AppUser user = userRepository.findFirstByEmailIgnoreCase(userDetails.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            String secret = tfaService.generateNewSecret();
            user.setTwoFactorSecret(secret);
            userRepository.save(user);

            String qrCodeUri = tfaService.generateQrCodeImageUri(secret, user.getEmail());
            log.info("[2FA] Setup initiated for {}", user.getEmail());
            return ResponseEntity.ok(new TwoFactorSetupResponse(secret, qrCodeUri));

        } catch (Exception e) {
            log.error("[2FA] Setup failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("2FA setup failed. Please try again."));
        }
    }

    // ─── 2FA Toggle ────────────────────────────────────────────────────────────

    @PostMapping("/2fa/toggle")
    public ResponseEntity<?> toggle2FA(@RequestBody Map<String, Boolean> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponse("Not authenticated"));
            }

            AppUser user = userRepository.findFirstByEmailIgnoreCase(userDetails.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            boolean enable = Boolean.TRUE.equals(request.get("enable"));

            if (enable && (user.getTwoFactorSecret() == null || user.getTwoFactorSecret().isBlank())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Please complete 2FA setup before enabling it."));
            }

            user.setTwoFactorEnabled(enable);
            if (!enable) {
                user.setTwoFactorSecret(null); // clear secret when disabling
            }
            userRepository.save(user);

            log.info("[2FA] {} for user {}", enable ? "Enabled" : "Disabled", user.getEmail());
            return ResponseEntity.ok(new MessageResponse(
                    "Two-factor authentication " + (enable ? "enabled" : "disabled") + " successfully."));

        } catch (Exception e) {
            log.error("[2FA] Toggle failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to update 2FA settings."));
        }
    }

    // ─── 2FA Verification ──────────────────────────────────────────────────────

    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verify2FA(
            @RequestBody VerifyTwoFactorRequest request,
            HttpServletRequest httpRequest) {

        String email = request.getEmail();

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Email is required for 2FA verification."));
        }

        // ── Lockout check (5 failed attempts per 5-minute window) ─────────────
        if (totpAttemptService.isLocked(email)) {
            log.warn("[2FA] OTP verification locked for {}", email);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse(
                            "Too many failed verification attempts. Please wait 5 minutes."));
        }

        AppUser user = userRepository.findFirstByEmailIgnoreCase(email).orElse(null);

        if (user == null || user.getTwoFactorSecret() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Invalid 2FA request."));
        }

        // ── OTP validation ────────────────────────────────────────────────────
        boolean valid = tfaService.isOtpValid(user.getTwoFactorSecret(), request.getCode());

        if (!valid) {
            int remaining = totpAttemptService.recordFailedAttempt(email) > 0
                    ? totpAttemptService.getRemainingAttempts(email)
                    : 0;
            log.warn("[2FA] Invalid OTP for {} – {} attempts remaining", email, remaining);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse(
                            "Invalid verification code. " + remaining + " attempts remaining."));
        }

        // ── Success: clear lockout counter, issue JWT ─────────────────────────
        totpAttemptService.clearAttempts(email);

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

        log.info("[2FA] Verification successful for {} – JWT issued", email);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(new UserInfoResponse(
                        userDetails.getId(),
                        userDetails.getUsername(),
                        userDetails.getEmail(),
                        user.isTwoFactorEnabled()));
    }

    // ─── Utilities ─────────────────────────────────────────────────────────────

    /**
     * Extracts the real client IP, respecting common reverse-proxy headers.
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
