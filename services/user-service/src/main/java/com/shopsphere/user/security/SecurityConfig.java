package com.shopsphere.user.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

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
    registration.setEnabled(false);  // prevent auto-registration as servlet filter
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
    .requestMatchers(HttpMethod.GET, "/users/me").authenticated()
    .requestMatchers(HttpMethod.PUT, "/users/me").authenticated()
    .requestMatchers(HttpMethod.GET, "/users", "/users/by-email", "/users/*").hasAuthority("ROLE_ADMIN")
    .anyRequest().authenticated()
)
            .addFilterBefore(headerAuthFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


 
}