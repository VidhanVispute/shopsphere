package com.shopsphere.gateway.filter;

import com.shopsphere.common.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * GlobalFilter runs on EVERY request through the Gateway.
 * This is where JWT validation happens — once, centrally.
 *
 * Flow:
 * 1. Is this a public endpoint? → skip JWT check, forward
 * 2. Is Authorization header present? → if not, 401
 * 3. Is the JWT valid (signature + expiry)? → if not, 401
 * 4. JWT is valid → extract claims, add to request headers, forward
 *
 * WHY add claims to headers?
 * Downstream services need to know WHO is making the request.
 * They trust the Gateway validated the JWT already.
 * So Gateway extracts userId, role from JWT and forwards them
 * as custom headers: X-User-Id, X-User-Role, X-User-Email.
 * Downstream services read headers — never parse JWT themselves.
 */
@Slf4j
@Component
@Order(1) // Run this filter first, before all other filters
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;

    // Endpoints that don't require authentication
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/vendor/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/products",          // browsing products is public
            "/api/products/",         // product detail is public
            "/api/search",            // search is public
            "/actuator/health"        // health checks are public
    );

    // Admin creation endpoint — blocked completely at gateway
    private static final List<String> BLOCKED_ENDPOINTS = List.of(
            "/api/admin/create"
    );

   public JwtAuthFilter(
        @Value("${jwt.public-key}") String publicKey,
        @Value("${jwt.access-token-expiration-ms}") long expirationMs) {
    this.jwtUtils = new JwtUtils(publicKey, expirationMs);
}

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        log.debug("Gateway processing: {} {}", method, path);

        // Step 1 — Block forbidden endpoints entirely
        // /api/admin/create must never reach any service from outside
        if (isBlocked(path)) {
            log.warn("Blocked request to forbidden endpoint: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        // Step 2 — Skip JWT check for public endpoints
        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        // Step 3 — All other endpoints require a valid JWT
        String authHeader = request.getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        // Step 4 — Validate the JWT
        if (!jwtUtils.isTokenValid(token)) {
            log.warn("Invalid or expired JWT for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Step 5 — JWT is valid. Extract claims and forward as headers.
        // Downstream services read these headers instead of parsing JWT.
        String userId = String.valueOf(jwtUtils.extractUserId(token));
        String role = jwtUtils.extractRole(token);
        String email = jwtUtils.extractEmail(token);

        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", role)
                .header("X-User-Email", email)
                .build();

        log.debug("JWT validated for userId={}, role={}", userId, role);

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    @Override
    public int getOrder() {
        return 1;
    }

    private boolean isPublic(String path) {
        return PUBLIC_ENDPOINTS.stream()
                .anyMatch(endpoint -> path.equals(endpoint)
                        || path.startsWith(endpoint));
    }

    private boolean isBlocked(String path) {
        return BLOCKED_ENDPOINTS.stream()
                .anyMatch(path::startsWith);
    }
}