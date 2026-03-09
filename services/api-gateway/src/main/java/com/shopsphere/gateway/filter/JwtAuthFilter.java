package com.shopsphere.gateway.filter;

import com.shopsphere.common.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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

    // Fully public — no JWT required, any HTTP method
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/vendor/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/search",
            "/actuator/health"
    );

    // Public for GET only — browsing products and categories
    // /my sub-path is explicitly excluded (requires VENDOR role)
    private static final List<String> PUBLIC_GET_PATHS = List.of(
            "/api/products",
            "/api/categories"
    );

    // Blocked completely — never forwarded to any service
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

        // Step 1 — Hard block certain endpoints
        if (isBlocked(path)) {
            log.warn("Blocked request to forbidden endpoint: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        // Step 2 — Fully public endpoints, skip JWT entirely
        if (isFullyPublic(path)) {
            return chain.filter(exchange);
        }

        // Step 3 — Public GET paths (products, categories)
        // but NOT sub-paths like /my which require authentication
        if (HttpMethod.GET.name().equals(method) && isPublicGet(path)) {
            return chain.filter(exchange);
        }

        // Step 4 — Everything else requires a valid JWT
        String authHeader = request.getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        if (!jwtUtils.isTokenValid(token)) {
            log.warn("Invalid or expired JWT for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Step 5 — Valid JWT → extract claims and forward as trusted headers
        String userId = String.valueOf(jwtUtils.extractUserId(token));
        String role   = jwtUtils.extractRole(token);
        String email  = jwtUtils.extractEmail(token);

        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id",    userId)
                .header("X-User-Role",  role)
                .header("X-User-Email", email)
                .build();

        log.debug("JWT validated for userId={}, role={}", userId, role);

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    @Override
    public int getOrder() {
        return 1;
    }

    // Fully public — exact match or known prefix (auth endpoints)
    private boolean isFullyPublic(String path) {
        return PUBLIC_ENDPOINTS.stream()
                .anyMatch(endpoint -> path.equals(endpoint)
                        || path.startsWith(endpoint + "/")
                        || path.equals(endpoint));
    }

    // Public GET only — /api/products and /api/categories
    // Excludes any path containing /my (e.g. /api/products/my)
    private boolean isPublicGet(String path) {
        if (path.contains("/my")) return false;
        return PUBLIC_GET_PATHS.stream()
                .anyMatch(prefix -> path.equals(prefix)
                        || path.startsWith(prefix + "/"));
    }

    private boolean isBlocked(String path) {
        return BLOCKED_ENDPOINTS.stream()
                .anyMatch(path::startsWith);
    }
}