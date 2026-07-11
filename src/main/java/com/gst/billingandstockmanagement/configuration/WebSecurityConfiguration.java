package com.gst.billingandstockmanagement.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.gst.billingandstockmanagement.filters.JwtRequestFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableScheduling
public class WebSecurityConfiguration {

    @Autowired
    private JwtRequestFilter authFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(management -> management
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(authorize -> authorize
                        // Public Endpoints
                        .requestMatchers(
                                "/health-check/**",
                                "/authenticate",
                                "/sign-up",
                                "/refresh-token",
                                "/forgot-password",
                                "/reset-password",
                                "/verify-email",
                                "/resend-verification",
                                "/auth/google"
                        ).permitAll()

                        // Product-request endpoints:
                        //   /submit and /my are open to any authenticated user;
                        //   /pending, /all, /approve, /reject are locked to ADMIN
                        //   via @PreAuthorize on the controller methods.
                        .requestMatchers("/api/**").authenticated()

                        .anyRequest().authenticated()
                )

                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}