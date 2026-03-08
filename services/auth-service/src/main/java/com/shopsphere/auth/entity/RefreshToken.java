package com.shopsphere.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash; // SHA-256 hash of raw token — raw never stored

    @Column(name = "family_id", nullable = false)
    private UUID familyId;
    /*
     * WHY family_id?
     * Token rotation: every /refresh issues a new token and revokes the old one.
     * If an attacker steals an old token and replays it, the system detects:
     * "this token was already used" → the entire family is revoked.
     * All devices sharing this login session are forced to re-authenticate.
     * Without family tracking, you could only revoke the single replayed token,
     * leaving the attacker's newer token still valid.
     */

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}