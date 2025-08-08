package com.orderingsystem.util;

import com.orderingsystem.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final String issuer;

    public JwtUtil(
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

    public String createAccessToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getUserId().toString())
                .issuer(issuer)
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTtl.toMillis()))
                .claim("userId", user.getUserId().toString())
                .claim("typ", "access")
                .signWith(secretKey, SIG.HS256)
                .compact();
    }

    public String createRefreshToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getUserId().toString())
                .issuer(issuer)
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshTtl.toMillis()))
                .claim("typ", "refresh")
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public boolean isValidRefreshToken(String token){
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

}
