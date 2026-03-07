package com.shopsphere.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Logs every request and response passing through the Gateway.
 * Runs BEFORE JwtAuthFilter (Order 0) so we log everything,
 * including rejected requests.
 *
 * In production, these logs flow to the ELK stack.
 * You can then query: "all requests to order-service in the last hour"
 * or "all 401 responses from the gateway today".
 */
@Slf4j
@Component
@Order(0)
public class LoggingFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();

        log.info("→ {} {}", method, path);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value()
                    : 0;
            log.info("← {} {} {} ({}ms)", method, path, statusCode, duration);
        }));
    }
}