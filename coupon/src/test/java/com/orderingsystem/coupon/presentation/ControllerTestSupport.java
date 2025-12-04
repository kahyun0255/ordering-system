package com.orderingsystem.coupon.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.UserType;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${jwt.secret-key}")
    protected String secretKey;

    @Value("${jwt.issuer}")
    protected String issuer;

    protected String buildToken(UUID userId, UserType userType) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .claim("userId", userId.toString())
                .claim("typ", "access")
                .claim("role", userType.name())
                .expiration(Date.from(Instant.now().plusSeconds(10000)))
                .issuedAt(new Date())
                .signWith(key, SIG.HS256)
                .compact();
    }

}
