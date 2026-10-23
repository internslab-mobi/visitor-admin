package com.adminvisitor.auth.security;

import com.adminvisitor.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Spring Security filter executed once per request to authenticate incoming HTTP requests using JWT cookies.
 * <p>
 * Extracts the access token from the {@code ACCESS_TOKEN} cookie, validates its cryptographic RS256 signature,
 * verifies that the user is active in the database, maps authorities based on the token state (e.g., standard
 * roles vs restricted first-login password change), and populates the {@link SecurityContextHolder}.
 * </p>
 *
 * @author Logeshwaran
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Special authority granted to users authenticated with a restricted token who must complete an initial password change.
     */
    public static final String INITIAL_PASSWORD_CHANGE_AUTHORITY =
            "ROLE_INITIAL_PASSWORD_CHANGE_REQUIRED";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Constructs the JWT authentication filter with required JWT service and user repository dependencies.
     *
     * @param jwtService service used to validate tokens and parse JWT claims
     * @param userRepository repository used to verify account existence and active status
     */
    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    /**
     * Filters each incoming HTTP request, extracting the JWT access token cookie and establishing
     * the security context authentication if the token is valid and the user account is active.
     *
     * @param request the HTTP servlet request
     * @param response the HTTP servlet response
     * @param filterChain the remaining filter chain to execute
     * @throws ServletException if a servlet error occurs during processing
     * @throws IOException if an I/O error occurs during processing
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String jwt = getAccessTokenFromCookie(request);

        if (jwt == null || jwt.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!jwtService.isTokenValid(jwt)) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            Claims claims = jwtService.parseClaims(jwt);

            String userId = claims.getSubject();

            if (userId == null
                    || !userRepository.existsByIdAndStatus(userId, "ACTIVE")) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            String role = claims.get("role", String.class);
            String state = claims.get("state", String.class);

            List<SimpleGrantedAuthority> authorities;

            if (JwtService.STATE_INITIAL_PASSWORD_CHANGE_REQUIRED
                    .equals(state)) {

                authorities = Collections.singletonList(
                        new SimpleGrantedAuthority(
                                INITIAL_PASSWORD_CHANGE_AUTHORITY
                        )
                );

            } else if (JwtService.STATE_AUTHENTICATED.equals(state)) {

                if (!"ADMIN".equals(role) && !"HR".equals(role)) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

            } else {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                authorities
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authToken);
            }

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the raw JWT access token string from the {@code ACCESS_TOKEN} cookie on the incoming request.
     *
     * @param request the HTTP servlet request
     * @return the access token string, or {@code null} if the cookie is not present
     */
    private String getAccessTokenFromCookie(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("ACCESS_TOKEN".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}