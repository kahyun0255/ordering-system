package com.orderingsystem.order.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.order.application.dto.response.CreateOrderResponse;
import com.orderingsystem.order.domain.model.Customer;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.restaurant.Product;
import com.orderingsystem.order.domain.model.restaurant.Restaurant;
import com.orderingsystem.order.domain.model.restaurant.RestaurantProduct;
import com.orderingsystem.order.domain.repository.CustomerRepository;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.repository.restaurant.ProductRepository;
import com.orderingsystem.order.domain.repository.restaurant.RestaurantProductRepository;
import com.orderingsystem.order.domain.repository.restaurant.RestaurantRepository;
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

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RestaurantProductRepository restaurantProductRepository;

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

        productRepository.save(Product.builder()
                .productId(productId1)
                .available(true)
                .name("product1")
                .price(product1Price)
                .build());

        productRepository.save(Product.builder()
                .productId(productId2)
                .available(true)
                .name("product2")
                .price(product2Price)
                .build());

        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .active(true)
                .name("restaurant1")
                .build());

        restaurantProductRepository.save(RestaurantProduct.builder()
                .id(UUID.randomUUID())
                .restaurantId(restaurantId)
                .productId(productId1)
                .build());

        restaurantProductRepository.save(RestaurantProduct.builder()
                .id(UUID.randomUUID())
                .restaurantId(restaurantId)
                .productId(productId2)
                .build());
    }

    @AfterEach
    void tearDown() {
        customerRepository.deleteAllInBatch();
        restaurantProductRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        restaurantRepository.deleteAllInBatch();
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

    @DisplayName("레스토랑이 존재하지 않는다면 NotFound를 반환하고, 주문이 실패한다.")
    @Test
    void failToCreateOrder_whenRestaurantDoesNotExist() throws Exception {
        //given
        CreateOrderRequest createOrderRequest =
                getCreateOrderRequest(UUID.randomUUID(), productId1, product1Price, productId2, product2Price);
        String token = buildToken(customerId, "access", issuer, Instant.now().plusSeconds(100000));

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
                .andExpect(jsonPath("$.message").value("레스토랑 정보를 찾을 수 없습니다."))
                .andReturn();

        long after = orderRepository.count();

        assertThat(before).isEqualTo(after);
    }

    @DisplayName("상품이 존재하지 않는다면 NotFound를 반환하고, 주문이 실패한다.")
    @Test
    void failToCreateOrder_whenProductDoesNotExist() throws Exception {
        //given
        CreateOrderRequest createOrderRequest =
                getCreateOrderRequest(restaurantId, UUID.randomUUID(), product1Price);
        String token = buildToken(customerId, "access", issuer, Instant.now().plusSeconds(100000));

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
                .andExpect(jsonPath("$.message").value("상품 정보를 찾을 수 없습니다."))
                .andReturn();

        long after = orderRepository.count();

        assertThat(before).isEqualTo(after);
    }

    @DisplayName("상품 중 하나라도 존재하지 않는다면 NotFound를 반환하고, 주문이 실패한다.")
    @Test
    void failToCreateOrder_whenAnyProductDoesNotExist() throws Exception {
        //given
        CreateOrderRequest createOrderRequest =
                getCreateOrderRequest(restaurantId, UUID.randomUUID(), product1Price, productId2, product2Price);
        String token = buildToken(customerId, "access", issuer, Instant.now().plusSeconds(100000));

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
                .andExpect(jsonPath("$.message").value("요청한 상품 중 일부를 찾을 수 없습니다."))
                .andReturn();

        long after = orderRepository.count();

        assertThat(before).isEqualTo(after);
    }

    @DisplayName("상품가격이 일치하지 않으면 주문은 생성되지만, 바로 취소된다.")
    @Test
    void cancelOrderImmediately_whenProductPriceMismatch() throws Exception {
        //given
        CreateOrderRequest createOrderRequest =
                getCreateOrderRequest(restaurantId, productId2, new Money(new BigDecimal("1000.00")), productId2,
                        product2Price);
        String token = buildToken(customerId, "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/order")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(createOrderRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderTrackingId").isNotEmpty())
                .andExpect(jsonPath("$.orderStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.message").value("주문 요청이 유효하지 않아 주문이 완료되지 않았습니다."))
                .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        CreateOrderResponse createOrderResponse = objectMapper.readValue(json, CreateOrderResponse.class);

        Optional<Order> order = orderRepository.findByTrackingId(createOrderResponse.getOrderTrackingId());
        assertThat(order).isPresent();
        assertThat(order.get().getCustomerId()).isEqualTo(customerId);
        assertThat(order.get().getRestaurantId()).isEqualTo(restaurantId);
        assertThat(order.get().getPrice().getAmount()).isEqualTo(createOrderRequest.getPrice());
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @DisplayName("개별 상품의 합과 주문의 총 가격이 일치하지 않으면 주문은 생성되지만, 바로 취소된다.")
    @Test
    void cancelOrderImmediately_whenOrderTotalPriceMismatch() throws Exception {
        //given
        CreateOrderRequest createOrderRequest = CreateOrderRequest.builder()
                .restaurantId(restaurantId)
                .address(OrderAddressRequest.builder()
                        .street("street1")
                        .postalCode("123-78")
                        .city("city1")
                        .build())
                .price(product1Price.multiply(2).getAmount())
                .items(List.of(OrderItemRequest.builder()
                        .productId(productId1)
                        .quantity(1)
                        .price(product1Price.getAmount())
                        .subTotal(product1Price.getAmount())
                        .build()))
                .build();

        String token = buildToken(customerId, "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/order")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(createOrderRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderTrackingId").isNotEmpty())
                .andExpect(jsonPath("$.orderStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.message").value("주문 요청이 유효하지 않아 주문이 완료되지 않았습니다."))
                .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        CreateOrderResponse createOrderResponse = objectMapper.readValue(json, CreateOrderResponse.class);

        Optional<Order> order = orderRepository.findByTrackingId(createOrderResponse.getOrderTrackingId());
        assertThat(order).isPresent();
        assertThat(order.get().getCustomerId()).isEqualTo(customerId);
        assertThat(order.get().getRestaurantId()).isEqualTo(restaurantId);
        assertThat(order.get().getPrice().getAmount()).isEqualTo(createOrderRequest.getPrice());
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
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
