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

@Service
public class JwtService {

    public static final String STATE_INITIAL_PASSWORD_CHANGE_REQUIRED =
            "INITIAL_PASSWORD_CHANGE_REQUIRED";

    public static final String STATE_AUTHENTICATED = "AUTHENTICATED";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long expiration;
    private final long refreshExpiration;

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

    public String generateToken(String userId, String role) {
        return buildToken(userId, role, STATE_AUTHENTICATED, expiration);
    }

    public String generateRestrictedToken(String userId, String role) {
        return buildToken(
                userId,
                role,
                STATE_INITIAL_PASSWORD_CHANGE_REQUIRED,
                expiration
        );
    }

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

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String extractState(String token) {
        return parseClaims(token).get("state", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpiration() {
        return expiration;
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

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
