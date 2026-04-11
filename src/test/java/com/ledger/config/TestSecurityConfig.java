package com.ledger.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration that disables authentication for integration tests.
 * This configuration permits all requests without authentication.
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    /**
     * Creates a permissive security filter chain for testing.
     * All requests are permitted without authentication.
     *
     * @param http the HttpSecurity to configure
     * @return the test security filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .headers(headers -> headers.disable());

        return http.build();
    }

    /**
     * Provides a test JwtService bean.
     *
     * @return JwtService for testing
     */
    @Bean
    @Primary
    public JwtService jwtService() {
        return new JwtService();
    }
}




