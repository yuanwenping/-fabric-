package com.changde.trace.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expireHours;

    public JwtService(
            @Value("${app.jwt-secret}") String secret,
            @Value("${app.jwt-expire-hours:12}") long expireHours
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireHours = expireHours;
    }

    public String issue(CurrentUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim("userType", "ENTERPRISE")
                .claim("username", user.username())
                .claim("orgId", user.orgId())
                .claim("orgType", user.orgType())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expireHours, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    public String issueConsumer(Long consumerId, String phone) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(consumerId))
                .claim("userType", "CONSUMER")
                .claim("phone", phone)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expireHours, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    public TokenData parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String userType = claims.get("userType", String.class);
        return new TokenData(
                Long.valueOf(claims.getSubject()),
                userType == null ? "ENTERPRISE" : userType
        );
    }

    public record TokenData(Long id, String userType) {
    }
}
