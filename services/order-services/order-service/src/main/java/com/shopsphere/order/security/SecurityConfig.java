package com.shopsphere.order.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public HeaderAuthFilter headerAuthFilter() {
        return new HeaderAuthFilter();
    }

    // Prevents Spring Boot from auto-registering the filter
    // outside of the security chain (would run twice otherwise)
    @Bean
    public FilterRegistrationBean<HeaderAuthFilter> headerAuthFilterRegistration() {
        FilterRegistrationBean<HeaderAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(headerAuthFilter());
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(headerAuthFilter(), UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/error").permitAll()

                // Cart endpoints — customers only
                .requestMatchers(HttpMethod.GET,    "/cart").hasAuthority("ROLE_USER")
                .requestMatchers(HttpMethod.POST,   "/cart").hasAuthority("ROLE_USER")
                .requestMatchers(HttpMethod.PUT,    "/cart/*").hasAuthority("ROLE_USER")
                .requestMatchers(HttpMethod.DELETE, "/cart/*").hasAuthority("ROLE_USER")
                .requestMatchers(HttpMethod.DELETE, "/cart").hasAuthority("ROLE_USER")

                // Order endpoints — customers only
                .requestMatchers(HttpMethod.POST, "/orders").hasAuthority("ROLE_USER")
                .requestMatchers(HttpMethod.GET,  "/orders").hasAuthority("ROLE_USER")
                .requestMatchers(HttpMethod.GET,  "/orders/*").hasAuthority("ROLE_USER")
                .requestMatchers(HttpMethod.POST, "/orders/*/cancel").hasAuthority("ROLE_USER")

                // Admin endpoints — admins only
                .requestMatchers(HttpMethod.GET, "/admin/orders").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/admin/orders/*/status").hasAuthority("ROLE_ADMIN")

                .anyRequest().authenticated()
            );

        return http.build();
    }
}