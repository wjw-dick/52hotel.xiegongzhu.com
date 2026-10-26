package com.hotel.saas.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expireHours;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-hours:12}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireHours = expireHours;
    }

    public String create(TenantContext.User u) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(u.userId()))
                .claim("username", u.username())
                .claim("tenantId", u.tenantId())
                .claim("tenantCode", u.tenantCode())
                .claim("role", u.role())
                .claim("platform", u.platform())
                .claim("storeId", u.storeId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expireHours, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    public TenantContext.User parse(String token) {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Long storeId = c.get("storeId") == null ? null : Long.valueOf(c.get("storeId").toString());
        return new TenantContext.User(
                Long.valueOf(c.getSubject()),
                c.get("username", String.class),
                Long.valueOf(c.get("tenantId").toString()),
                c.get("tenantCode", String.class),
                c.get("role", String.class),
                Boolean.TRUE.equals(c.get("platform", Boolean.class)),
                storeId);
    }
}
