package com.ojt.p01.config;

import com.ojt.p01.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cấu hình Spring Security 6 (Stateless JWT).
 *
 * Access Matrix (SRS §02 §5):
 *
 * /api/v1/auth/**                   → public (login)
 * /api/v1/categories                → tất cả đã xác thực
 * /api/v1/tickets/**                → tất cả đã xác thực (phân quyền data trong service)
 * /api/v1/admin/sla-configs GET     → IT_MANAGER + ADMIN
 * /api/v1/admin/**                  → ADMIN only
 * /v3/api-docs/**, /swagger-ui/**   → public (dev tools)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // --- Public ---
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // --- SLAConfig: GET cho IT_MANAGER + ADMIN, PUT chỉ ADMIN ---
                .requestMatchers(HttpMethod.GET, "/api/v1/admin/sla-configs/**")
                    .hasAnyRole("IT_MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/admin/sla-configs/**")
                    .hasRole("ADMIN")

                // --- Admin endpoints: ADMIN only ---
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // --- Authenticated endpoints ---
                .requestMatchers("/api/v1/categories/**").authenticated()
                .requestMatchers("/api/v1/tickets/**").authenticated()

                // --- Mọi request khác cần xác thực ---
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

        return http.build();
    }

    /**
     * Băm mật khẩu BCrypt cost 12 (SDD §2.1, HD01).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
