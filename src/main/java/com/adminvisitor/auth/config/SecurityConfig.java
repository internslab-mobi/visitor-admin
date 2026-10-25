package com.adminvisitor.auth.config;

import com.adminvisitor.auth.security.CustomAuthenticationEntryPoint;
import com.adminvisitor.auth.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Core security configuration for the Authentication Service.
 * <p>
 * Configures the Spring Security filter chain, establishing stateless session management,
 * Cross-Origin Resource Sharing (CORS), CSRF protection, URL authorization rules,
 * the JWT authentication filter, authentication manager, and password encoder.
 * </p>
 *
 * @author Logeshwaran
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    /**
     * Constructs the security configuration with required authentication filter and entry point dependencies.
     *
     * @param jwtAuthenticationFilter the custom JWT filter for request token validation
     * @param customAuthenticationEntryPoint the entry point handling unauthorized authentication failures
     */
    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
    }

    /**
     * Defines the security filter chain governing HTTP access controls, CSRF configuration,
     * stateless session policy, exception handling, and filter ordering.
     *
     * @param http the {@link HttpSecurity} object to configure
     * @return the assembled {@link SecurityFilterChain}
     * @throws Exception if an error occurs while configuring HTTP security
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.spa())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(customAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/error",
                                "/api/auth/csrf",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/forgot-password",
                                "/api/auth/forgot-password/resend",
                                "/api/auth/verify-reset-otp",
                                "/api/auth/reset-password",
                                "/swagger-ui/**",
                                "/favicon.ico",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/api/auth/users").hasRole("ADMIN")
                        .requestMatchers("/api/auth/change-password").hasAnyAuthority(
                                JwtAuthenticationFilter.INITIAL_PASSWORD_CHANGE_AUTHORITY,
                                "ROLE_ADMIN",
                                "ROLE_HR"
                        )
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * Configures CORS permissions allowing designated client origins, HTTP methods,
     * request headers, and credentials for {@code /api/**} routes.
     *
     * @return the configured {@link CorsConfigurationSource}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }

    /**
     * Exposes the {@link AuthenticationManager} bean from Spring Security's authentication configuration.
     *
     * @param configuration the Spring Security {@link AuthenticationConfiguration}
     * @return the configured {@link AuthenticationManager}
     * @throws Exception if an error occurs during retrieval
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }


    /**
     * Provides the {@link PasswordEncoder} bean utilizing BCrypt hashing algorithm for credential storage and validation.
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
