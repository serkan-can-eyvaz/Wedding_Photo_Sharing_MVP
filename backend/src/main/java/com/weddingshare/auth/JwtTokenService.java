package com.weddingshare.auth;

import com.weddingshare.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtTokenService {

    private static final int MINIMUM_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final Duration accessTokenTtl;

    public JwtTokenService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-ttl}") Duration accessTokenTtl
    ) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes long");
        }
        if (accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalStateException("JWT access token TTL must be positive");
        }

        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.accessTokenTtl = accessTokenTtl;
    }

    public LoginResponse createToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(accessTokenTtl);
        String token = Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        return new LoginResponse(token, expiresAt);
    }

    public String validateAndGetSubject(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Date expiration = claims.getExpiration();
        if (expiration == null || !expiration.toInstant().isAfter(Instant.now())) {
            throw new JwtException("JWT is expired");
        }

        return claims.getSubject();
    }
}
