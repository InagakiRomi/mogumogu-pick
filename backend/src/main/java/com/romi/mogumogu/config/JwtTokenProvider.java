package com.romi.mogumogu.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.romi.mogumogu.entity.user.UserEntity;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long expirationTime;

    /**
     * @param secretKey      簽章金鑰
     * @param expirationTime 有效期限（毫秒）
     */
    public JwtTokenProvider(String secretKey, long expirationTime) {
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }

    /** 產生 access token。 */
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
}
