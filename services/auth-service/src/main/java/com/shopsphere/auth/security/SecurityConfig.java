package com.shopsphere.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /*
     * WHY is Auth Service's security config simple compared to other services?
     *
     * Auth Service sits BEHIND the API Gateway.
     * The Gateway is the security boundary — it validates JWTs and blocks
     * unauthenticated requests before they ever reach any service.
     *
     * Auth Service itself only has PUBLIC endpoints (login, register, etc.)
     * by definition — you can't require a JWT to log in.
     *
     * So Auth Service opens all /api/auth/** endpoints publicly,
     * and locks everything else with denyAll() as a safety net.
     *
     * The Gateway enforces the real security perimeter.
     * This config is the backstop in case something bypasses the Gateway.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // Auth service is stateless — no sessions, JWT only
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/auth/register",
                    "/auth/vendor/register",
                    "/auth/login",
                    "/auth/refresh",
                    "/auth/forgot-password",
                    "/auth/reset-password",
                    "/auth/verify-email/**",
                    "/.well-known/jwks.json",  // public key endpoint
                    "/actuator/health",
                    "/auth/logout"
                ).permitAll()
                .anyRequest().denyAll()
                /*
                 * denyAll() not authenticated() — important distinction.
                 * authenticated() would require a valid Spring Security
                 * authentication object, which we don't set up here
                 * (the Gateway handles that).
                 * denyAll() means: if a request reaches Auth Service
                 * on any unlisted path, reject it unconditionally.
                 * Belt and suspenders.
                 */
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Cost factor 12 — ~250ms on modern hardware.
        // High enough to make brute-force impractical,
        // low enough not to bottleneck login under normal load.
        return new BCryptPasswordEncoder(12);
    }
}