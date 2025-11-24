package com.orderingsystem.order.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.order.domain.model.Customer;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.OrderItem;
import com.orderingsystem.order.domain.repository.CustomerRepository;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.repository.outbox.PaymentOutboxRepository;
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
import java.util.stream.Stream;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class OrderControllerCancelOrderTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;

    @Autowired
    private MockMvc mockMvc;

    @Value("${jwt.issuer}")
    protected String issuer;

    @Value("${jwt.secret-key}")
    protected String secretKey;

    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        customerRepository.save(Customer.builder()
                .id(customerId)
                .name("유저")
                .build());
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
        paymentOutboxRepository.deleteAllInBatch();
    }

    @DisplayName("주문을 한 유저가 주문 취소를 요청하면 PENDING, PAID, ACCEPTED 상태라면 주문 취소를 접수한다.")
    @ParameterizedTest(name = "[{index}] 주문 상태 : {0}")
    @MethodSource("provideCancellableOrderStatuses")
    void shouldCancelOrder_whenUserIsOrdererAndStatusIsCancellable(String status, OrderStatus orderStatus)
            throws Exception {
        //given
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .price(new Money(BigDecimal.valueOf(1000)))
                .address(UUID.randomUUID())
                .orderStatus(orderStatus)
                .customerId(customerId)
                .trackingId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .items(List.of(OrderItem.builder()
                        .subTotal(new Money(BigDecimal.valueOf(1000)))
                        .quantity(10)
                        .productId(UUID.randomUUID())
                        .price(new Money(BigDecimal.valueOf(1000)))
                        .build()))
                .build();

        orderRepository.save(order);

        String token = buildToken(customerId);

        //when
        mockMvc.perform(
                        post("/api/order/" + order.getTrackingId() + "/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        //then
        Optional<Order> afterOrder = orderRepository.findById(order.getId());
        assertThat(afterOrder).isPresent();
        assertThat(afterOrder.get().getOrderStatus()).isEqualTo(OrderStatus.CANCELLING);

        assertThat(paymentOutboxRepository.count()).isOne();
    }

    private static Stream<Arguments> provideCancellableOrderStatuses() {
        return Stream.of(
                Arguments.of("PENDING", OrderStatus.PENDING),
                Arguments.of("PAID", OrderStatus.PAID),
                Arguments.of("ACCEPTED", OrderStatus.ACCEPTED)
        );
    }

    @DisplayName("주문을 한 유저가 주문 취소를 요청할 때 APPROVED, CANCELLING, CANCELLED 상태라면 주문 취소를 접수할 수 없고, 400을 반환한다.")
    @ParameterizedTest(name = "[{index}] 주문 상태 : {0}")
    @MethodSource("provideNonCancellableOrderStatuses")
    void shouldReturn400_whenCancellingNonCancellableOrder(String status, OrderStatus orderStatus) throws Exception {
        //given
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .price(new Money(BigDecimal.valueOf(1000)))
                .address(UUID.randomUUID())
                .orderStatus(orderStatus)
                .customerId(customerId)
                .trackingId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .items(List.of(OrderItem.builder()
                        .subTotal(new Money(BigDecimal.valueOf(1000)))
                        .quantity(10)
                        .productId(UUID.randomUUID())
                        .price(new Money(BigDecimal.valueOf(1000)))
                        .build()))
                .build();

        orderRepository.save(order);

        String token = buildToken(customerId);

        //when, then
        mockMvc.perform(
                        post("/api/order/" + order.getTrackingId() + "/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("주문을 취소할 수 없는 상태입니다."));

        Optional<Order> afterOrder = orderRepository.findById(order.getId());
        assertThat(afterOrder).isPresent();
        assertThat(afterOrder.get().getOrderStatus()).isEqualTo(orderStatus);
    }

    private static Stream<Arguments> provideNonCancellableOrderStatuses() {
        return Stream.of(
                Arguments.of("APPROVED", OrderStatus.APPROVED),
                Arguments.of("CANCELLING", OrderStatus.CANCELLING),
                Arguments.of("CANCELLED", OrderStatus.CANCELLED)
        );
    }

    @DisplayName("주문 취소를 요청한 유저가 존재하지 않으면 404를 반환하고, 주문 취소에 실패한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExist() throws Exception {
        //given
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .price(new Money(BigDecimal.valueOf(1000)))
                .address(UUID.randomUUID())
                .orderStatus(OrderStatus.ACCEPTED)
                .customerId(customerId)
                .trackingId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .items(List.of(OrderItem.builder()
                        .subTotal(new Money(BigDecimal.valueOf(1000)))
                        .quantity(10)
                        .productId(UUID.randomUUID())
                        .price(new Money(BigDecimal.valueOf(1000)))
                        .build()))
                .build();

        orderRepository.save(order);

        String token = buildToken(UUID.randomUUID());

        //when, then
        mockMvc.perform(
                        post("/api/order/" + order.getTrackingId() + "/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("주문자를 찾을 수 없습니다."));

        Optional<Order> afterOrder = orderRepository.findById(order.getId());
        assertThat(afterOrder).isPresent();
        assertThat(afterOrder.get().getOrderStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @DisplayName("trackingId에 해당하는 주문이 존재하지 않으면 404를 반환하고, 주문 취소에 실패한다.")
    @Test
    void shouldReturn404_whenOrderWithTrackingIdDoesNotExist() throws Exception {
        //given
        String token = buildToken(customerId);

        //when, then
        mockMvc.perform(
                        post("/api/order/" + UUID.randomUUID() + "/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("주문 내역을 찾을 수 없습니다."));
    }

    @DisplayName("주문을 신청한 유저와 주문 취소를 요청한 유저가 다르면 403을 반환하고, 주문 취소에 실패한다.")
    @Test
    void shouldReturn403_whenCancellingOrderByNonOwner() throws Exception {
        //given
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .price(new Money(BigDecimal.valueOf(1000)))
                .address(UUID.randomUUID())
                .orderStatus(OrderStatus.ACCEPTED)
                .customerId(UUID.randomUUID())
                .trackingId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .items(List.of(OrderItem.builder()
                        .subTotal(new Money(BigDecimal.valueOf(1000)))
                        .quantity(10)
                        .productId(UUID.randomUUID())
                        .price(new Money(BigDecimal.valueOf(1000)))
                        .build()))
                .build();

        orderRepository.save(order);

        String token = buildToken(customerId);

        //when, then
        mockMvc.perform(
                        post("/api/order/" + order.getTrackingId() + "/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("주문을 취소할 권한이 없습니다."));

        Optional<Order> afterOrder = orderRepository.findById(order.getId());
        assertThat(afterOrder).isPresent();
        assertThat(afterOrder.get().getOrderStatus()).isEqualTo(OrderStatus.ACCEPTED);
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

