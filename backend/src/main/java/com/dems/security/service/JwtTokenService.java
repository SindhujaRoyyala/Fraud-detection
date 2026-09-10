package com.dems.security.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class JwtTokenService {

    private final String jwtSecret;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    private final String issuer;
    private final StringRedisTemplate redisTemplate;

    private SecretKey signingKey;

    public JwtTokenService(
            @Value("${app.security.jwt.secret}") String jwtSecret,
            @Value("${app.security.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${app.security.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs,
            @Value("${app.security.jwt.issuer}") String issuer,
            StringRedisTemplate redisTemplate) {
        this.jwtSecret = jwtSecret;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.issuer = issuer;
        this.redisTemplate = redisTemplate;
        init();
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            keyBytes = padded;
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UUID userId, String username, Set<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("type", "ACCESS")
                .claim("username", username)
                .claim("roles", new ArrayList<>(roles))
                .id(UUID.randomUUID().toString())
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);
        String tokenId = UUID.randomUUID().toString();

        String refreshToken = Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("type", "REFRESH")
                .id(tokenId)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        try {
            redisTemplate.opsForValue().set(
                    "refresh_token:" + tokenId,
                    userId.toString(),
                    refreshTokenExpirationMs,
                    TimeUnit.MILLISECONDS
            );
        } catch (Exception e) {
            log.warn("Could not store refresh token in Redis, proceeding without: {}", e.getMessage());
        }

        return refreshToken;
    }

    public Claims validateAndParseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserIdFromClaims(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String getUsernameFromClaims(Claims claims) {
        return claims.get("username", String.class);
    }

    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromClaims(Claims claims) {
        List<String> rolesList = claims.get("roles", List.class);
        if (rolesList == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(rolesList);
    }

    public String getTokenType(Claims claims) {
        return claims.get("type", String.class);
    }

    public String getTokenId(Claims claims) {
        return claims.getId();
    }

    public boolean isTokenRevoked(String tokenId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey("revoked_token:" + tokenId));
        } catch (Exception e) {
            log.warn("Could not check token revocation in Redis: {}", e.getMessage());
            return false;
        }
    }

    public void revokeAccessToken(String tokenId, long expirationMs) {
        try {
            redisTemplate.opsForValue().set(
                    "revoked_token:" + tokenId,
                    "true",
                    expirationMs,
                    TimeUnit.MILLISECONDS
            );
        } catch (Exception e) {
            log.warn("Could not revoke token in Redis: {}", e.getMessage());
        }
    }

    public boolean validateRefreshToken(String tokenId, UUID expectedUserId) {
        try {
            String storedUserId = redisTemplate.opsForValue().get("refresh_token:" + tokenId);
            return storedUserId != null && storedUserId.equals(expectedUserId.toString());
        } catch (Exception e) {
            log.warn("Could not validate refresh token in Redis: {}", e.getMessage());
            return true;
        }
    }

    public void deleteRefreshToken(String tokenId) {
        try {
            redisTemplate.delete("refresh_token:" + tokenId);
        } catch (Exception e) {
            log.warn("Could not delete refresh token from Redis: {}", e.getMessage());
        }
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}
