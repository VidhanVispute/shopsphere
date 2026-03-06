package com.shopsphere.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT utility shared between Auth Service and API Gateway.
 *
 * Auth Service uses it to: generate tokens after login
 * API Gateway uses it to: validate tokens on every incoming request
 *
 * WHY HMAC-SHA256 (HS256)?
 * JWT can be signed with symmetric (same key for sign+verify = HS256)
 * or asymmetric (private key signs, public key verifies = RS256).
 * HS256 is simpler and fast. RS256 is better when multiple independent
 * parties need to verify tokens without sharing a secret.
 * For our setup — Gateway and Auth Service are both internal — HS256 is fine.
 *
 * IMPORTANT: The secret key must be at least 256 bits (32 characters).
 * This class does NOT store the secret — it receives it as a parameter.
 * The actual secret lives in application.yml (or Kubernetes Secret in prod).
 */
public class JwtUtils {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtils(String secret, long expirationMs) {
        // Keys.hmacShaKeyFor ensures the key meets minimum length requirements
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email, String role, Long userId) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    public boolean isTokenValid(String token) {
        try {
            return !extractAllClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            // Any parsing or signature failure means the token is invalid
            return false;
        }
    }
}