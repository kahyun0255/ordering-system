package com.orderingsystem.restaurant.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.restaurant.domain.repository.OwnerRepository;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantOwnershipRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantProductRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected RestaurantRepository restaurantRepository;

    @Autowired
    protected OwnerRepository ownerRepository;

    @Autowired
    protected RestaurantOwnershipRepository restaurantOwnershipRepository;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected RestaurantProductRepository restaurantProductRepository;

    @Value("${jwt.issuer}")
    protected String issuer;

    @Value("${jwt.secret-key}")
    protected String secretKey;

    @AfterEach
    void tearDown() {
        restaurantRepository.deleteAllInBatch();
        ownerRepository.deleteAllInBatch();
        restaurantOwnershipRepository.deleteAllInBatch();
    }

    protected String buildToken(UUID userId, String typ, String iss, Instant exp) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(iss)
                .claim("userId", userId.toString())
                .claim("typ", typ)
                .expiration(Date.from(exp))
                .issuedAt(new Date())
                .signWith(key, SIG.HS256)
                .compact();
    }

    protected String buildToken(UUID userId) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .claim("userId", userId.toString())
                .claim("typ", "access")
                .expiration(Date.from(Instant.now().plusSeconds(10000)))
                .issuedAt(new Date())
                .signWith(key, SIG.HS256)
                .compact();
    }

    protected String buildToken(UUID userId, UserType userType) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .claim("userId", userId.toString())
                .claim("role", userType.name())
                .claim("typ", "access")
                .expiration(Date.from(Instant.now().plusSeconds(10000)))
                .issuedAt(new Date())
                .signWith(key, SIG.HS256)
                .compact();
    }

}
