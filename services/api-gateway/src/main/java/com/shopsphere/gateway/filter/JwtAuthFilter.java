package com.shopsphere.gateway.filter;

import com.shopsphere.common.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@Order(1)
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;

    // Fully public endpoints
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/",
            "/api/search",
            "/actuator/health"
    );

    // Public GET endpoints
    private static final List<String> PUBLIC_GET_PATHS = List.of(
            "/api/products",
            "/api/categories",
            "/api/inventory"
    );

    // Completely blocked endpoints
    private static final List<String> BLOCKED_ENDPOINTS = List.of(
            "/api/admin/create"
    );

    public JwtAuthFilter(
            @Value("${jwt.public-key}") String publicKey,
            @Value("${jwt.access-token-expiration-ms}") long expirationMs) {

        log.info("JWT PUBLIC KEY LOADED");
        this.jwtUtils = new JwtUtils(publicKey, expirationMs);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        log.info("Gateway request: {} {}", method, path);

        // 1. Block dangerous endpoints
        if (isBlocked(path)) {
            log.warn("Blocked forbidden endpoint: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        // 2. Allow fully public endpoints
        if (isFullyPublic(path)) {
            return chain.filter(exchange);
        }

        // 3. Allow public GET browsing
        if (method == HttpMethod.GET && isPublicGet(path)) {
            return chain.filter(exchange);
        }

        // 4. Require JWT
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing Authorization header for {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        if (!jwtUtils.isTokenValid(token)) {
            log.warn("Invalid or expired JWT");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 5. Extract claims
        String userId = String.valueOf(jwtUtils.extractUserId(token));
        String rawRole = jwtUtils.extractRole(token);
        String email = jwtUtils.extractEmail(token);

        // 6. Normalize role for Spring Security
        String role = mapRole(rawRole);

        // 7. Inject trusted headers
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", rawRole)
                .header("X-User-Email", email)
                .build();

        log.debug("JWT valid → userId={} role={}", userId, role);

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    @Override
    public int getOrder() {
        return 1;
    }

    // ---------------- Helper Methods ----------------

    private boolean isFullyPublic(String path) {
        return PUBLIC_ENDPOINTS.stream()
                .anyMatch(path::startsWith);
    }

    private boolean isPublicGet(String path) {
        if (path.contains("/my")) return false;

        return PUBLIC_GET_PATHS.stream()
                .anyMatch(prefix ->
                        path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    private boolean isBlocked(String path) {
        return BLOCKED_ENDPOINTS.stream()
                .anyMatch(path::startsWith);
    }

    private String mapRole(String role) {

        return switch (role) {
            case "CUSTOMER" -> "ROLE_USER";
            case "ADMIN" -> "ROLE_ADMIN";
            case "VENDOR" -> "ROLE_VENDOR";
            default -> "ROLE_USER";
        };
    }
}