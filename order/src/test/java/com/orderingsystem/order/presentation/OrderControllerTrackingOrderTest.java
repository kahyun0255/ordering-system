package com.orderingsystem.order.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.OrderItem;
import com.orderingsystem.order.domain.repository.OrderRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class OrderControllerTrackingOrderTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MockMvc mockMvc;

    @Value("${jwt.issuer}")
    protected String issuer;

    @Value("${jwt.secret-key}")
    protected String secretKey;

    private final UUID customerId = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        orderRepository.deleteAllInBatch();
    }

    @DisplayName("주문자가 주문 추적을 요청하면 성공적으로 주문 상태를 반환한다.")
    @Test
    void shouldReturn200_whenCustomerTracksOwnOrder() throws Exception {
        //given
        Order order = orderRepository.save(createOrder(customerId));
        String token = buildToken(customerId);

        //when, then
        mockMvc.perform(
                        get("/api/orders/" + order.getTrackingId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderTrackingId").value(order.getTrackingId().toString()))
                .andExpect(jsonPath("$.orderStatus").value(OrderStatus.PENDING.name()));
    }

    @DisplayName("주문자가 아닌 유저가 주문 추적을 요청하면 404를 반환한다.")
    @Test
    void shouldReturn404_whenTrackingOrderByNonOwner() throws Exception {
        //given
        Order order = orderRepository.save(createOrder(customerId));
        String token = buildToken(UUID.randomUUID());

        //when, then
        mockMvc.perform(
                        get("/api/orders/" + order.getTrackingId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("trackingId에 대한 주문을 찾을 수 없습니다."));
    }

    @DisplayName("trackingId에 해당하는 주문이 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenOrderWithTrackingIdDoesNotExist() throws Exception {
        //given
        String token = buildToken(UUID.randomUUID());

        //when, then
        mockMvc.perform(
                        get("/api/orders/" + UUID.randomUUID())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("trackingId에 대한 주문을 찾을 수 없습니다."));
    }

    private Order createOrder(UUID ordererId) {
        return Order.builder()
                .id(UUID.randomUUID())
                .price(new Money(BigDecimal.valueOf(1000)))
                .address(UUID.randomUUID())
                .orderStatus(OrderStatus.PENDING)
                .customerId(ordererId)
                .trackingId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .items(List.of(OrderItem.builder()
                        .subTotal(new Money(BigDecimal.valueOf(1000)))
                        .quantity(10)
                        .productId(UUID.randomUUID())
                        .price(new Money(BigDecimal.valueOf(1000)))
                        .build()))
                .build();
    }

    private String buildToken(UUID userId) {
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

}
