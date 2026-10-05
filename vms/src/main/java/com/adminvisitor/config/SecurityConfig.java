package com.adminvisitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${jwt.secret}") String secret
    ) {
        SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        return NimbusJwtDecoder
                .withSecretKey(secretKey)
                .build();
    }

    @Bean
    public CookieAccessTokenFilter cookieAccessTokenFilter() {
        return new CookieAccessTokenFilter();
    }

    /*
     * Prevent Spring Boot from registering this filter as a regular
     * servlet filter. It must run only inside the Spring Security chain.
     */
    @Bean
    public FilterRegistrationBean<CookieAccessTokenFilter>
    cookieAccessTokenFilterRegistration(
            CookieAccessTokenFilter filter
    ) {
        FilterRegistrationBean<CookieAccessTokenFilter> registration =
                new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CookieAccessTokenFilter cookieAccessTokenFilter,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
            CustomAccessDeniedHandler customAccessDeniedHandler
    ) throws Exception {

        http
                .cors(Customizer.withDefaults())

                // CSRF disabled for this local learning configuration.
                .csrf(csrf -> csrf.disable())

                .addFilterBefore(
                        cookieAccessTokenFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                customAuthenticationEntryPoint
                        )
                        .accessDeniedHandler(
                                customAccessDeniedHandler
                        )
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/health",
                                "/api/csrf",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/visitors")
                        .hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(HttpMethod.GET, "/api/visitors/*")
                        .hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(HttpMethod.PUT, "/api/visitors/*")
                        .hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(HttpMethod.GET, "/api/visits")
                        .hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(HttpMethod.POST, "/api/visits")
                        .hasRole("FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/visits/*/check-in"
                        ).hasRole("FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/visits/*/check-out"
                        ).hasRole("FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/visits/*/cancel"
                        ).hasRole("FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/visit-badges/*"
                        ).hasRole("FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/visit-badges/validate"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/documents/visitor/*"
                        ).hasRole("FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/documents/api/proof-documents/upload/*"
                        ).hasRole("FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/documents/*/nda"
                        ).hasRole("FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/documents/*/nda/download"
                        ).hasRole("FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/documents/*/nda/all"
                        ).hasRole("FRONT_DESK")

                        .requestMatchers(HttpMethod.GET, "/api/vendors")
                        .hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(HttpMethod.GET, "/api/vendors/*")
                        .hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(HttpMethod.GET, "/api/employees")
                        .hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(HttpMethod.GET, "/api/departments")
                        .hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers("/api/blacklist/**")
                        .hasRole("ADMIN")

                        .anyRequest().permitAll()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(
                                new DefaultBearerTokenResolver()
                        )
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                jwtAuthenticationConverter()
                        ))
                );

        return http.build();
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken>
    jwtAuthenticationConverter() {

        return jwt -> {
            String state = jwt.getClaimAsString("state");

            if (!"AUTHENTICATED".equals(state)) {
                throw new BadCredentialsException(
                        "JWT is not permitted for normal VMS access"
                );
            }

            String role = jwt.getClaimAsString("role");

            if (!"ADMIN".equals(role) && !"FRONT_DESK".equals(role)) {
                throw new BadCredentialsException("Invalid role in JWT");
            }

            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

            return new JwtAuthenticationToken(
                    jwt,
                    authorities,
                    jwt.getSubject()
            );
        };
    }
}