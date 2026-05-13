package com.xiaoyu.yinran.security;

import com.xiaoyu.yinran.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final AppProperties appProperties;

    public String generateToken(Long adminId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("adminId", adminId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(appProperties.getJwtExpireHours(), ChronoUnit.HOURS)))
                .signWith(secretKey())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey secretKey() {
        String secret = appProperties.getJwtSecret();
        if (secret.length() < 32) {
            secret = String.format("%-32s", secret).replace(' ', 'x');
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}

