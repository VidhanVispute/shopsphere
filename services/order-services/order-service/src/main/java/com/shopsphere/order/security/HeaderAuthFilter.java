package com.shopsphere.order.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class HeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");
        String userEmail = request.getHeader("X-User-Email");

        if (userId != null && userRole != null) {

    String role = switch (userRole) {
    case "CUSTOMER" -> "ROLE_USER";
    case "ADMIN"    -> "ROLE_ADMIN";
    case "VENDOR"   -> "ROLE_VENDOR";
    default         -> "ROLE_USER";
};

    var authorities = Collections.singletonList(
        new SimpleGrantedAuthority(role)  
);

    var authentication = new UsernamePasswordAuthenticationToken(
            userId, userEmail, authorities
    );

    SecurityContextHolder.getContext().setAuthentication(authentication);
}

        filterChain.doFilter(request, response);
    }
}