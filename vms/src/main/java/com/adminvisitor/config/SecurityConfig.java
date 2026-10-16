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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

import java.util.function.Supplier;

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
    public CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();

        repository.setCookieName("VMS-XSRF-TOKEN");
        repository.setHeaderName("X-VMS-XSRF-TOKEN");
        repository.setCookiePath("/");

        return repository;
    }

    private static final class SpaCsrfTokenRequestHandler
            implements CsrfTokenRequestHandler {

        private final CsrfTokenRequestHandler plain =
                new CsrfTokenRequestAttributeHandler();

        private final CsrfTokenRequestHandler xor =
                new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(
                HttpServletRequest request,
                HttpServletResponse response,
                Supplier<CsrfToken> csrfToken
        ) {
            this.xor.handle(request, response, csrfToken);

            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(
                HttpServletRequest request,
                CsrfToken csrfToken
        ) {
            String headerValue =
                    request.getHeader(csrfToken.getHeaderName());

            return (headerValue != null)
                    ? this.plain.resolveCsrfTokenValue(request, csrfToken)
                    : this.xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CsrfTokenRepository csrfTokenRepository,
            CookieAccessTokenFilter cookieAccessTokenFilter,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
            CustomAccessDeniedHandler customAccessDeniedHandler
    ) throws Exception {

        http
                .cors(Customizer.withDefaults())

                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                )

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

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/visitors"
                        ).hasAnyRole("ADMIN","FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/visitors/*"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/visitors/*/edit"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/visitors/*"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/visits"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/visits"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/visits/*/check-in"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.PATCH, "/api/visits/*/check-out"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/visits/*/cancel"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/visit-badges/*"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/visit-badges/validate"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/documents/visitor/*"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/documents/api/proof-documents/upload/*"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/documents/*/nda/download"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/documents/*/nda/all"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/vendors"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/vendors/*"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/employees"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/departments"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/blacklist/visitor/*/status"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/blacklist",
                                "/api/blacklist/*"
                        ).hasAnyRole("ADMIN", "FRONT_DESK")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/blacklist",
                                "/api/blacklist/visitor/*"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/blacklist/*/remove"
                        ).hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(
                                new DefaultBearerTokenResolver()
                        )
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
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