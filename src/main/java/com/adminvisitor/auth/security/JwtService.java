package com.adminvisitor.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * Provides JWT generation, parsing, and validation using RSA key pairs.
 * Supports authenticated tokens and restricted tokens for users who must
 * change their initial password.
 *
 * @author Logeshwaran
 */
@Service
public class JwtService {

    public static final String STATE_INITIAL_PASSWORD_CHANGE_REQUIRED =
            "INITIAL_PASSWORD_CHANGE_REQUIRED";

    public static final String STATE_AUTHENTICATED = "AUTHENTICATED";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long expiration;
    private final long refreshExpiration;

    /**
     * Initializes the JWT service by loading the RSA keys and configuring
     * the access-token and refresh-token expiration durations.
     *
     * @param privateKeyPath the file path of the RSA private key
     * @param publicKeyPath the file path of the RSA public key
     * @param expiration the access-token expiration duration in milliseconds
     * @param refreshExpiration the refresh-token expiration duration in milliseconds
     * @throws IllegalStateException if either RSA key cannot be loaded
     */
    public JwtService(
            @Value("${jwt.private-key-path}") String privateKeyPath,
            @Value("${jwt.public-key-path}") String publicKeyPath,
            @Value("${jwt.expiration}") long expiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration
    ) {
        this.privateKey = loadPrivateKey(privateKeyPath);
        this.publicKey = loadPublicKey(publicKeyPath);
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
    }

    /**
     * Generates a JWT for a fully authenticated user.
     *
     * @param userId the unique identifier of the user
     * @param role the role assigned to the user
     * @return the signed JWT with the authenticated state
     */
    public String generateToken(String userId, String role) {
        return buildToken(userId, role, STATE_AUTHENTICATED, expiration);
    }

    /**
     * Generates a restricted JWT for a user who must change their initial password.
     *
     * @param userId the unique identifier of the user
     * @param role the role assigned to the user
     * @return the signed JWT with the initial-password-change-required state
     */
    public String generateRestrictedToken(String userId, String role) {
        return buildToken(
                userId,
                role,
                STATE_INITIAL_PASSWORD_CHANGE_REQUIRED,
                expiration
        );
    }

    /**
     * Builds and signs a JWT containing the user identifier, role, authentication
     * state, issue time, and expiration time.
     *
     * @param userId the unique identifier of the user
     * @param role the role assigned to the user
     * @param state the authentication state represented by the token
     * @param ttlMillis the token lifetime in milliseconds
     * @return the signed JWT
     */
    private String buildToken(
            String userId,
            String role,
            String state,
            long ttlMillis
    ) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMillis);

        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .claim("state", state)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Parses a signed JWT and verifies its signature using the RSA public key.
     *
     * @param token the JWT to parse
     * @return the claims contained in the verified token
     * @throws JwtException if the token is invalid, expired, or its signature
     *         cannot be verified
     * @throws IllegalArgumentException if the supplied token is invalid as an argument
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the subject identifying the user from a JWT.
     *
     * @param token the JWT containing the user identifier
     * @return the user identifier stored as the token subject
     */
    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extracts the user's role from a JWT.
     *
     * @param token the JWT containing the role claim
     * @return the role stored in the token
     */
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * Extracts the authentication state from a JWT.
     *
     * @param token the JWT containing the state claim
     * @return the authentication state stored in the token
     */
    public String extractState(String token) {
        return parseClaims(token).get("state", String.class);
    }

    /**
     * Checks whether a JWT can be parsed and has a future expiration time.
     *
     * @param token the JWT to validate
     * @return {@code true} if the token is valid and unexpired; {@code false} otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Returns the configured access-token expiration duration.
     *
     * @return the access-token lifetime in milliseconds
     */
    public long getExpiration() {
        return expiration;
    }

    /**
     * Returns the configured refresh-token expiration duration.
     *
     * @return the refresh-token lifetime in milliseconds
     */
    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    /**
     * Loads an RSA private key from a PEM-encoded file using the PKCS#8 format.
     *
     * @param path the file path of the private key
     * @return the loaded RSA private key
     * @throws IllegalStateException if the file cannot be read or the key cannot
     *         be decoded or converted
     */
    private static PrivateKey loadPrivateKey(String path) {
        try {
            String pem = Files.readString(
                    Path.of(path),
                    StandardCharsets.UTF_8
            );

            String encoded = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(encoded);

            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        } catch (IOException | GeneralSecurityException
                 | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Unable to load JWT RSA private key", e
            );
        }
    }

    /**
     * Loads an RSA public key from a PEM-encoded file using the X.509 format.
     *
     * @param path the file path of the public key
     * @return the loaded RSA public key
     * @throws IllegalStateException if the file cannot be read or the key cannot
     *         be decoded or converted
     */
    private static PublicKey loadPublicKey(String path) {
        try {
            String pem = Files.readString(
                    Path.of(path),
                    StandardCharsets.UTF_8
            );

            String encoded = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(encoded);

            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(keyBytes));

        } catch (IOException | GeneralSecurityException
                 | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Unable to load JWT RSA public key", e
            );
        }
    }
}