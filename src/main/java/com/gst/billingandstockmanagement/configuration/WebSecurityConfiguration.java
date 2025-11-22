package com.gst.billingandstockmanagement.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// IMPORTANT: The import below is often needed for the new lambda style
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
public class WebSecurityConfiguration {

    @Autowired
    private JwtRequestFilter authFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Use the lambda-based configuration to remove deprecation warnings

        http
                // 1. Disable CSRF (using lambda)
                // Replaces: http.csrf().disable()
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Configure Session Management (using lambda)
                // Replaces: .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .sessionManagement(management -> management
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 3. Configure URL Authorization (using lambda)
                // Replaces: .authorizeHttpRequests().requestMatchers(...).permitAll().and().authorizeHttpRequests().requestMatchers(...).authenticated().and()
                .authorizeHttpRequests(authorize -> authorize
                        // Public Endpoints
                        .requestMatchers("/health-check", "/authenticate", "/sign-up", "/refresh-token").permitAll()
                        // Authenticated Endpoints
                        .requestMatchers("/api/**").authenticated()
                        // Catch any other requests (often not necessary if the above covers all)
                        .anyRequest().authenticated()
                )

                // 4. Add Custom Filter (remains mostly the same)
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