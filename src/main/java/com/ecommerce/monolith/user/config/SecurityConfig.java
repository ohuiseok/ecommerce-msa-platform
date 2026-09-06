package com.ecommerce.monolith.user.config;

import com.ecommerce.monolith.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(
                        "/api/users/register",
                        "/api/users/login",
                        "/api/users/health",
                        "/api/products/health",
                        "/api/orders/health",
                        "/api/cart/health",
                        "/api/payments/health",
                        "/api/payments/webhooks/mock-pg",
                        "/api/coupons/health",
                        "/api/reviews/health",
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/orders/status/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/orders/*/status").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/payments/reconciliation/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/payments/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/coupons/my").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/coupons").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/coupons/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reviews/my").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
