package com.orderingsystem.order.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.Money;
import com.orderingsystem.order.application.port.out.RestaurantApi;
import com.orderingsystem.order.application.dto.response.CreateOrderResponse;
import com.orderingsystem.order.domain.model.Customer;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.repository.CustomerRepository;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.presentation.request.CreateOrderRequest;
import com.orderingsystem.order.presentation.request.OrderAddressRequest;
import com.orderingsystem.order.presentation.request.OrderItemRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("OrderController CreateOrder 통합 테스트")
class OrderControllerCreateOrderTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RestaurantApi restaurantApi;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.secret-key}")
    private String secretKey;

    private final UUID restaurantId = UUID.randomUUID();
    private final Money product1Price = new Money(new BigDecimal("20.00"));
    private final Money product2Price = new Money(new BigDecimal("25.00"));
    private final UUID productId1 = UUID.randomUUID();
    private final UUID productId2 = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        customerRepository.save(Customer.builder()
                .id(customerId)
                .name("테스트유저")
                .build());
    }

    @AfterEach
    void tearDown() {
        customerRepository.deleteAllInBatch();
        orderRepository.deleteAll();
    }

    @DisplayName("주문 생성에 성공한다.")
    @Test
    void crateOrder() throws Exception {
        //given
        CreateOrderRequest createOrderRequest =
                getCreateOrderRequest(restaurantId, productId1, product1Price, productId2, product2Price);
        String token = buildToken(customerId, "access", issuer, Instant.now().plusSeconds(100000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/order")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(createOrderRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderTrackingId").isNotEmpty())
                .andExpect(jsonPath("$.orderStatus").value("PENDING"))
                .andExpect(jsonPath("$.message").value("주문이 성공적으로 생성되었습니다."))
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString();
        CreateOrderResponse createOrderResponse = objectMapper.readValue(json, CreateOrderResponse.class);

        Optional<Order> order = orderRepository.findByTrackingId(createOrderResponse.getOrderTrackingId());
        assertThat(order).isPresent();
        assertThat(order.get().getCustomerId()).isEqualTo(customerId);
        assertThat(order.get().getRestaurantId()).isEqualTo(restaurantId);
        assertThat(order.get().getPrice().getAmount()).isEqualTo(createOrderRequest.getPrice());
    }

    @DisplayName("AccessToken이 만료되면 401 Unauthorized를 반환하고, 주문이 실패한다.")
    @Test
    void failToCreateOrder_whenAccessTokenExpired() throws Exception {
        //given
        CreateOrderRequest createOrderRequest =
                getCreateOrderRequest(restaurantId, productId1, product1Price, productId2, product2Price);
        String token = buildToken(customerId, "access", issuer, Instant.now().minusSeconds(10));

        long before = orderRepository.count();

        //when, then
        mockMvc.perform(
                        post("/api/order")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(createOrderRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("AccessToken 검증에 실패했습니다."))
                .andReturn();

        long after = orderRepository.count();

        assertThat(before).isEqualTo(after);
    }

    @DisplayName("토큰이 accessToken이 아니라면 Unauthorized를 반환하고, 주문이 실패한다.")
    @Test
    void failToCreateOrder_whenTokenIsNotAccessToken() throws Exception {
        //given
        CreateOrderRequest createOrderRequest =
                getCreateOrderRequest(restaurantId, productId1, product1Price, productId2, product2Price);
        String token = buildToken(customerId, "refresh", issuer, Instant.now().plusSeconds(10000));

        long before = orderRepository.count();

        //when, then
        mockMvc.perform(
                        post("/api/order")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(createOrderRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("AccessToken 검증에 실패했습니다."))
                .andReturn();

        long after = orderRepository.count();

        assertThat(before).isEqualTo(after);
    }

    @DisplayName("로그인 한 유저가 존재하지 않는다면 NotFound를 반환하고, 주문이 실패한다.")
    @Test
    void failToCreateOrder_whenUserDoesNotExist() throws Exception {
        //given
        CreateOrderRequest createOrderRequest =
                getCreateOrderRequest(restaurantId, productId1, product1Price, productId2, product2Price);
        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(100000));

        long before = orderRepository.count();

        //when, then
        mockMvc.perform(
                        post("/api/order")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(createOrderRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("주문자를 찾을 수 없습니다."))
                .andReturn();

        long after = orderRepository.count();

        assertThat(before).isEqualTo(after);
    }

    private CreateOrderRequest getCreateOrderRequest(UUID restaurantId, UUID productId1, Money product1Price,
                                                     UUID productId2, Money product2Price) {
        return CreateOrderRequest.builder()
                .restaurantId(restaurantId)
                .address(OrderAddressRequest.builder()
                        .street("street1")
                        .postalCode("123-78")
                        .city("city1")
                        .build())
                .price(product2Price.multiply(2).getAmount().add(product1Price.getAmount()))
                .items(List.of(OrderItemRequest.builder()
                                .productId(productId1)
                                .quantity(1)
                                .price(product1Price.getAmount())
                                .subTotal(product1Price.getAmount())
                                .build(),
                        OrderItemRequest.builder()
                                .productId(productId2)
                                .quantity(2)
                                .price(product2Price.getAmount())
                                .subTotal(product2Price.multiply(2).getAmount())
                                .build()))
                .build();
    }

    private CreateOrderRequest getCreateOrderRequest(UUID restaurantId, UUID productId1, Money product1Price) {
        return CreateOrderRequest.builder()
                .restaurantId(restaurantId)
                .address(OrderAddressRequest.builder()
                        .street("street1")
                        .postalCode("123-78")
                        .city("city1")
                        .build())
                .price(product1Price.getAmount())
                .items(List.of(OrderItemRequest.builder()
                        .productId(productId1)
                        .quantity(1)
                        .price(product1Price.getAmount())
                        .subTotal(product1Price.getAmount())
                        .build()))
                .build();
    }

    private String buildToken(UUID userId, String typ, String iss, Instant exp) {
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

}
