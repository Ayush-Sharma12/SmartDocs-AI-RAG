package com.ayush.docsai.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(@Value("${app.jwt-secret}") String secret, @Value("${app.jwt-expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
        return true;
    }

    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        Object value = getClaims(token).get("userId");
        if (value instanceof Integer integerValue) {
            return integerValue.longValue();
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        return Long.valueOf(String.valueOf(value));
    }

    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }
}
