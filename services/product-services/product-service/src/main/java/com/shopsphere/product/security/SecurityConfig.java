package com.shopsphere.product.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(proxyTargetClass = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthFilter headerAuthFilter;

    @Bean
    public FilterRegistrationBean<HeaderAuthFilter> headerAuthFilterRegistration(
            HeaderAuthFilter filter) {
        FilterRegistrationBean<HeaderAuthFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // prevent double-registration
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers(HttpMethod.GET, "/products/my").hasAuthority("ROLE_VENDOR")
            .requestMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/categories", "/categories/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/products").hasAnyAuthority("ROLE_VENDOR", "ROLE_ADMIN")
            .requestMatchers(HttpMethod.PUT, "/products/*").hasAnyAuthority("ROLE_VENDOR", "ROLE_ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/products/*").hasAnyAuthority("ROLE_VENDOR", "ROLE_ADMIN")
            .requestMatchers(HttpMethod.POST, "/categories").hasAuthority("ROLE_ADMIN")
            .anyRequest().authenticated()
        )
            .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}