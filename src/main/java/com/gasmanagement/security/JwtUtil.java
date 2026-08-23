package com.gasmanagement.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utility class xử lý tạo và validate JWT token.
 * Sử dụng HS256 algorithm với secret key từ environment variable.
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    // ─────────────── GENERATE TOKEN ───────────────

    /**
     * Tạo Access Token từ username.
     * Claims bổ sung: role
     */
    public String generateAccessToken(String username, String role) {
        return buildToken(username, role, jwtExpiration);
    }

    /**
     * Tạo Refresh Token (không chứa role - chỉ dùng để renew access token).
     */
    public String generateRefreshToken(String username) {
        return buildToken(username, null, refreshExpiration);
    }

    // ─────────────── VALIDATE TOKEN ───────────────

    /**
     * Validate token - trả về true nếu token hợp lệ và chưa hết hạn.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (MalformedJwtException e) {
            log.warn("JWT token không hợp lệ: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token đã hết hạn: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token không được hỗ trợ: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims trống: {}", e.getMessage());
        }
        return false;
    }

    // ─────────────── EXTRACT CLAIMS ───────────────

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ─────────────── PRIVATE HELPERS ───────────────

    private String buildToken(String username, String role, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey());

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(jwtSecret.getBytes())
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
