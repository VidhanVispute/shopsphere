package com.shopsphere.auth.service;

import com.shopsphere.auth.dto.request.*;
import com.shopsphere.auth.dto.response.AuthResponse;
import com.shopsphere.auth.entity.PasswordResetToken;
import com.shopsphere.auth.entity.RefreshToken;
import com.shopsphere.auth.entity.User;
import com.shopsphere.auth.repository.PasswordResetTokenRepository;
import com.shopsphere.auth.repository.RefreshTokenRepository;
import com.shopsphere.auth.repository.UserRepository;
import com.shopsphere.auth.security.TokenHashUtil;
import com.shopsphere.common.events.PasswordResetRequestedEvent;
import com.shopsphere.common.events.UserRegisteredEvent;
import com.shopsphere.common.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JwtUtils jwtUtils;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

    // ── Registration ──────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse registerCustomer(RegisterRequest request) {
        validateEmailNotTaken(request.getEmail());

        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.CUSTOMER)
                .status(User.UserStatus.PENDING_VERIFICATION)
                .build();

        user = userRepository.save(user);
        log.info("Customer registered: userId={}", user.getId());

        publishUserRegisteredEvent(user, request.getName(), null);

        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse registerVendor(VendorRegisterRequest request) {
        validateEmailNotTaken(request.getEmail());

        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.VENDOR)
                // Vendors require admin approval before becoming ACTIVE
                .status(User.UserStatus.PENDING_VERIFICATION)
                .build();

        user = userRepository.save(user);
        log.info("Vendor registered: userId={}", user.getId());

        publishUserRegisteredEvent(user, request.getOwnerName(), request.getBusinessName());

        return issueTokenPair(user);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(this::invalidCredentials);
        /*
         * SECURITY: Same exception for wrong email AND wrong password.
         * If we returned "email not found" vs "wrong password" separately,
         * attackers could enumerate registered accounts by probing emails.
         * Intentionally vague error message prevents account enumeration.
         */

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw invalidCredentials();
        }

        if (user.getStatus() == User.UserStatus.BANNED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is banned");
        }

        if (user.getStatus() == User.UserStatus.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is suspended");
        }

        log.info("User logged in: userId={}", user.getId());
        return issueTokenPair(user);
    }

    // ── Token Refresh ─────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String tokenHash = TokenHashUtil.hash(request.getRefreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        // Reuse detection — if this token was already used (revoked), it's theft
        if (stored.isRevoked()) {
            log.warn("Refresh token reuse detected! Revoking family. userId={}",
                    stored.getUser().getId());
            refreshTokenRepository.revokeAllByFamilyId(stored.getFamilyId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Token reuse detected. Please log in again.");
        }

        if (stored.isExpired()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Refresh token expired. Please log in again.");
        }

        // Rotate: revoke current, issue new token in same family
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokenPairInFamily(stored.getUser(), stored.getFamilyId());
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Transactional
    public void logout(RefreshRequest request) {
        String tokenHash = TokenHashUtil.hash(request.getRefreshToken());

        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            // Revoke entire family — logs out all devices sharing this session
            refreshTokenRepository.revokeAllByFamilyId(token.getFamilyId());
            log.info("User logged out, family revoked: familyId={}", token.getFamilyId());
        });
        // If token not found — silent success. Don't reveal whether it existed.
    }

    // ── Password Reset ────────────────────────────────────────────────────────

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always return success even if email doesn't exist — prevents enumeration
        userRepository.findByEmail(request.getEmail().toLowerCase()).ifPresent(user -> {
            // Invalidate any existing unused reset tokens
            passwordResetTokenRepository.invalidateExistingTokensForUser(user.getId());

            String rawToken = TokenHashUtil.generateToken();
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(TokenHashUtil.hash(rawToken))
                    .expiresAt(expiresAt)
                    .used(false)
                    .build();

            passwordResetTokenRepository.save(resetToken);

            // Publish event — Notification Service sends the email
            publishPasswordResetEvent(user, rawToken, expiresAt);

            log.info("Password reset requested: userId={}", user.getId());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = TokenHashUtil.hash(request.getToken());

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid or expired reset token"));

        if (resetToken.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Reset token already used");
        }

        if (resetToken.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Reset token expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token used — single use enforced
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Revoke all refresh tokens — forces re-login on all devices after password change
        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("Password reset successful: userId={}", user.getId());
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private AuthResponse issueTokenPair(User user) {
        // New login = new family
        return issueTokenPairInFamily(user, UUID.randomUUID());
    }

    private AuthResponse issueTokenPairInFamily(User user, UUID familyId) {
        String accessToken = jwtUtils.generateAccessToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name());

        String rawRefreshToken = TokenHashUtil.generateToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHashUtil.hash(rawRefreshToken))
                .familyId(familyId)
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)  // raw token sent to client, hash stored in DB
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationMs / 1000)
                .role(user.getRole().name())
                .userId(user.getId().toString())
                .build();
    }

    private void validateEmailNotTaken(String email) {
        if (userRepository.existsByEmail(email.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email already registered");
        }
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Invalid credentials");
    }

    private void publishUserRegisteredEvent(User user, String name, String businessName) {
        /*
         * WHY fire-and-forget?
         * Kafka being down must NEVER fail a registration or login.
         * The user account is already persisted in the DB — the source of truth.
         * The Notification Service can replay missed events later if needed.
         * Letting Kafka block the HTTP thread for up to max.block.ms (60s default)
         * would cause timeouts and terrible UX just because a broker is unavailable.
         */
        try {
            kafkaTemplate.send("user-registered",
                    user.getId().toString(),
                    UserRegisteredEvent.builder()
                            .userId(user.getId())
                            .email(user.getEmail())
                            .name(name)
                            .role(user.getRole().name())
                            .businessName(businessName)
                            .registeredAt(user.getCreatedAt())
                            .build());
        } catch (Exception e) {
            log.warn("Failed to publish user-registered event for userId={}: {}",
                    user.getId(), e.getMessage());
            // Don't rethrow — Kafka down must never break auth
        }
    }

    private void publishPasswordResetEvent(User user, String rawToken, LocalDateTime expiresAt) {
        try {
            kafkaTemplate.send("password-reset-requested",
                    user.getId().toString(),
                    PasswordResetRequestedEvent.builder()
                            .userId(user.getId())
                            .email(user.getEmail())
                            .resetToken(rawToken)
                            .expiresAt(expiresAt)
                            .build());
        } catch (Exception e) {
            log.warn("Failed to publish password-reset-requested event for userId={}: {}",
                    user.getId(), e.getMessage());
            // Don't rethrow — password reset flow must still work without Kafka
        }
    }
}