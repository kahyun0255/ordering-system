package com.orderingsystem.common.util;

import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.common.exception.InvalidCredentialsException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CommonJwtUtil {

    private final SecretKey secretKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final String issuer;

    public CommonJwtUtil(
            @Value("${jwt.secret-key}") String base64Secret,
            @Value("${jwt.access-token-expiration}") Duration accessTtl,
            @Value("${jwt.refresh-token-expiration}") Duration refreshTtl,
            @Value("${jwt.issuer}") String issuer
    ) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
        this.issuer = issuer;
    }

    public boolean isValidRefreshToken(String token) {
        try {
            Claims claims = getClaims(token);
            return "refresh".equals(claims.get("typ", String.class))
                    && issuer.equals(claims.getIssuer());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserIdFromToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")){
            throw new InvalidCredentialsException("Authorization 헤더가 유효하지 않습니다.");
        }

        String accessToken = authorizationHeader.replace("Bearer ", "").trim();

        if (!isValidAccessToken(accessToken)) {
            throw new InvalidCredentialsException("AccessToken 검증에 실패했습니다.");
        }

        Claims claims = getClaims(accessToken);
        return UUID.fromString(claims.get("userId", String.class));
    }

    public UserType getUserRoleFromToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")){
            throw new InvalidCredentialsException("Authorization 헤더가 유효하지 않습니다.");
        }

        String accessToken = authorizationHeader.replace("Bearer ", "").trim();

        if (!isValidAccessToken(accessToken)) {
            throw new InvalidCredentialsException("AccessToken 검증에 실패했습니다.");
        }

        Claims claims = getClaims(accessToken);

        Object raw = claims.get("role");
        if (raw instanceof String s) {
            return UserType.valueOf(s.toUpperCase());
        }

        var list = claims.get("roles", java.util.List.class);
        if (list != null && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof String fs) return UserType.valueOf(fs.toUpperCase());
        }
        throw new InvalidCredentialsException("role 클레임이 없거나 형식이 잘못되었습니다.");
    }

    private boolean isValidAccessToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            Claims claims = getClaims(token);
            return "access".equals(claims.get("typ", String.class))
                    && issuer.equals(claims.getIssuer());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

}
