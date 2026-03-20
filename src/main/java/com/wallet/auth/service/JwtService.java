package com.wallet.auth.service;

import com.wallet.auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(User user) {
        return generateToken(user, jwtProperties.accessExpiryMs(),
                Map.of("type", "access"));
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, jwtProperties.refreshExpiryMs(),
                Map.of("type", "refresh"));
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractTokenType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    public boolean isTokenValid(String token, User user,
                                String expectedType) {
        Claims claims = parseClaims(token);
        return user.getEmail().equals(claims.getSubject())
                && expectedType.equals(claims.get("type", String.class))
                && claims.getExpiration().after(new Date());
    }

    public long getAccessExpiryMs() {
        return jwtProperties.accessExpiryMs();
    }

    private String generateToken(User user, long expiryMs,
                                 Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiryMs)))
                .signWith(signingKey())
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }
}

