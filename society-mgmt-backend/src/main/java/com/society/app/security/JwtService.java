package com.society.app.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT issuance + verification using jjwt 0.12.x.
 *
 * <p>PHASE-2 GAP: {@code refreshTokenStore} is in-memory and lost on restart.
 * Move to a {@code refresh_tokens} DB table in production (also enables
 * multi-instance deployments and revocation auditing).</p>
 */
@Service
public class JwtService {

    private final String secret;
    private final long accessExpiryMs;
    private final long refreshExpiryMs;

    /** In-memory refresh-token store: token -> phone. PHASE-2 GAP — see class Javadoc. */
    private final Map<String, String> refreshTokenStore = new ConcurrentHashMap<>();

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiry-ms}") long accessExpiryMs,
            @Value("${jwt.refresh-expiry-ms}") long refreshExpiryMs) {
        this.secret = secret;
        this.accessExpiryMs = accessExpiryMs;
        this.refreshExpiryMs = refreshExpiryMs;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Issue an access token for the given phone (subject). */
    public String generateAccessToken(String phone) {
        return buildToken(phone, accessExpiryMs);
    }

    /** Issue a refresh token and register it in the in-memory store. */
    public String generateRefreshToken(String phone) {
        String token = buildToken(phone, refreshExpiryMs);
        refreshTokenStore.put(token, phone);
        return token;
    }

    private String buildToken(String phone, long expiryMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(phone)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiryMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract the subject (phone) from a token. Throws {@link JwtException} on any
     * parse / signature / expiry failure — callers must handle.
     */
    public String extractPhone(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /** True if the access token parses and verifies; false on any JwtException. */
    public boolean isAccessTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * True only if the refresh token is present in the store AND still parses/verifies.
     * Logout (or refresh-rotation) removes the token from the store, which immediately
     * invalidates it for future use.
     */
    public boolean isRefreshTokenValid(String token) {
        if (!refreshTokenStore.containsKey(token)) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            refreshTokenStore.remove(token);
            return false;
        }
    }

    /** Remove a refresh token from the store. Safe to call on an unknown token. */
    public void invalidateRefreshToken(String token) {
        if (token != null) {
            refreshTokenStore.remove(token);
        }
    }
}
