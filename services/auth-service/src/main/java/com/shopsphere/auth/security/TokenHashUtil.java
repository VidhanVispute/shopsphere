package com.shopsphere.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class TokenHashUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generate a cryptographically secure random token.
     * 32 bytes = 256 bits of entropy — sufficient for refresh tokens.
     * Returned as URL-safe Base64 string.
     */
    public static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hash a token with SHA-256 for storage.
     * We store the hash, the client holds the raw token.
     * On verification: hash the incoming token, compare hashes.
     * Raw token never touches the database.
     */
    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the JVM spec — this never throws
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private TokenHashUtil() {}
}