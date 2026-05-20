package com.romi.mogumogu.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.romi.mogumogu.entity.user.UserEntity;
import com.romi.mogumogu.enums.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtTokenProvider {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey signingKey;
    private final long expirationTime;

    /**
     * 以 secret 建立 HMAC 簽章金鑰
     * 
     * @param secretKey      簽章金鑰
     * @param expirationTime 有效期限（毫秒）
     */
    public JwtTokenProvider(String secretKey, long expirationTime) {
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }

    /** 產生 access token */
    public String generateAccessToken(UserEntity user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationTime);
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("email", user.getEmail())
                .claim("groupId", user.getGroupId())
                .claim("role", user.getRoles().name())
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey)
                .compact();
    }

    /** 驗簽並解析 access token，無效或過期回傳 empty */
    public Optional<Claims> parseValidClaims(String compactJwt) {
        // 檢查 token 是否為空
        if (compactJwt == null || compactJwt.isBlank()) {
            return Optional.empty();
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clockSkewSeconds(60)
                    .build()
                    .parseSignedClaims(compactJwt.trim())
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /** 解析 Authorization header 並回傳 Authentication */
    public Optional<UsernamePasswordAuthenticationToken> resolveAuthentication(String authorizationHeader) {
        // 檢查 Authorization header 是否為空
        if (authorizationHeader == null
                || !authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return Optional.empty();
        }

        // 解析 JWT 並回傳 Authentication
        return parseValidClaims(authorizationHeader.substring(BEARER_PREFIX.length()).trim())
                .flatMap(claims -> UserRole.fromName(claims.get("role", String.class))
                        .map(role -> new UsernamePasswordAuthenticationToken(
                                claims.getSubject(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())))));
    }
}
