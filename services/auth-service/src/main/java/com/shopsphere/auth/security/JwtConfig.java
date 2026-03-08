package com.shopsphere.auth.security;

import com.shopsphere.common.util.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    public JwtUtils jwtUtils(
            @Value("${jwt.private-key}") String privateKey,
            @Value("${jwt.public-key}") String publicKey,
            @Value("${jwt.access-token-expiration-ms}") long expirationMs) {
        // Auth Service gets both keys — it signs and can verify
        return new JwtUtils(privateKey, publicKey, expirationMs);
    }
}