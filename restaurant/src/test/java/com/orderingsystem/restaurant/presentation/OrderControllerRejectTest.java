package com.orderingsystem.restaurant.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import com.orderingsystem.restaurant.domain.repository.OrderApprovalRepository;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import com.orderingsystem.restaurant.domain.repository.outbox.OrderOutboxRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class OrderControllerRejectTest extends ControllerTestSupport {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderApprovalRepository orderApprovalRepository;

    @Autowired
    private OrderOutboxRepository orderOutboxRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${key.stock}")
    private String stockKey;
    @Value("${key.reserved}")
    private String reserveKey;
    @Value("${key.confirmed}")
    private String confirmKey;

    private UUID productId1;
    private UUID productId2;
    private UUID orderId;
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        productId1 = UUID.randomUUID();
        productId2 = UUID.randomUUID();
        orderId = UUID.randomUUID();

        redisTemplate.opsForValue().set(stockKey + productId1, "10");
        redisTemplate.opsForValue().set(stockKey + productId2, "20");
        redisTemplate.opsForValue().set(reserveKey + productId1, "0");
        redisTemplate.opsForValue().set(reserveKey + productId2, "0");
        redisTemplate.opsForHash().put(confirmKey + orderId, productId1.toString(), "2");
        redisTemplate.opsForHash().put(confirmKey + orderId, productId2.toString(), "4");

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        OrderApproval orderApproval = OrderApproval.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .restaurantId(restaurantId)
                .status(OrderApprovalStatus.ACCEPTED)
                .build();
        orderApprovalRepository.save(orderApproval);

        Product product1 = Product.builder()
                .productId(productId1)
                .name("상품")
                .price(new Money(BigDecimal.valueOf(100)))
                .quantity(10)
                .available(true)
                .build();

        Product product2 = Product.builder()
                .productId(productId2)
                .name("상품")
                .price(new Money(BigDecimal.valueOf(10)))
                .quantity(20)
                .available(true)
                .build();

        productRepository.saveAll(List.of(product1, product2));

        Owner owner = Owner.builder()
                .userId(ownerId)
                .name("소유자")
                .build();
        ownerRepository.save(owner);
    }

    @AfterEach
    @Override
    void tearDown() {
        super.tearDown();
        productRepository.deleteAllInBatch();
        orderApprovalRepository.deleteAllInBatch();
        orderOutboxRepository.deleteAllInBatch();

        redisTemplate.delete(redisTemplate.keys("stock:*"));
        redisTemplate.delete(redisTemplate.keys("reserved:*"));
        redisTemplate.delete(redisTemplate.keys("history:*"));
        redisTemplate.delete(redisTemplate.keys("confirmed:*"));
    }

    @DisplayName("레스토랑의 소유자라면 주문 거절시 Redis, DB 재고가 복구되며 주문 상태가 REJECT가 된다.")
    @Test
    void shouldRejectOrderAndRestoreStock_whenUserIsRestaurantOwner() throws Exception {
        //given
        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .ownerId(ownerId)
                .restaurantId(restaurantId)
                .build());

        String token = buildToken(ownerId);

        //when
        mockMvc.perform(
                        post("/api/restaurants/" + restaurantId + "/orders/" + orderId + "/reject")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        //then
        Optional<Product> afterProduct1 = productRepository.findById(productId1);
        assertThat(afterProduct1).isPresent();
        assertThat(afterProduct1.get().getQuantity()).isEqualTo(12);

        Optional<Product> afterProduct2 = productRepository.findById(productId2);
        assertThat(afterProduct2).isPresent();
        assertThat(afterProduct2.get().getQuantity()).isEqualTo(24);

        Optional<OrderApproval> afterOrderApproval = orderApprovalRepository.findByOrderId(orderId);
        assertThat(afterOrderApproval).isPresent();
        assertThat(afterOrderApproval.get().getStatus()).isEqualTo(OrderApprovalStatus.REJECTED);

        String product1Stock = redisTemplate.opsForValue().get(stockKey + productId1);
        assertThat(product1Stock).isEqualTo("12");

        String product2Stock = redisTemplate.opsForValue().get(stockKey + productId2);
        assertThat(product2Stock).isEqualTo("24");

        assertThat(orderOutboxRepository.count()).isOne();
    }

    @DisplayName("레스토랑의 소유자가 아니라면 주문 거절이 불가능하고, 403을 반환한다.")
    @Test
    void shouldReturn403_whenUserIsNotRestaurantOwner() throws Exception {
        //given
        String token = buildToken(ownerId);

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + restaurantId + "/orders/" + orderId + "/reject")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 레스토랑의 주문을 거절할 권한이 없습니다."));

        Optional<OrderApproval> afterOrderApproval = orderApprovalRepository.findByOrderId(orderId);
        assertThat(afterOrderApproval).isPresent();
        assertThat(afterOrderApproval.get().getStatus()).isEqualTo(OrderApprovalStatus.ACCEPTED);
    }

    @DisplayName("레스토랑이 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenRestaurantDoesNotExist() throws Exception {
        //given
        String token = buildToken(ownerId);

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + UUID.randomUUID() + "/orders/" + orderId + "/reject")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("레스토랑 정보를 찾을 수 없습니다."));

        Optional<OrderApproval> afterOrderApproval = orderApprovalRepository.findByOrderId(orderId);
        assertThat(afterOrderApproval).isPresent();
        assertThat(afterOrderApproval.get().getStatus()).isEqualTo(OrderApprovalStatus.ACCEPTED);
    }

    @DisplayName("레스토랑이 삭제되었으면 주문 거절에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenRestaurantIsDeleted() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .name("삭제된 레스토랑")
                .status(RestaurantStatus.DELETED)
                .build();

        restaurantRepository.save(restaurant);

        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .ownerId(ownerId)
                .restaurantId(restaurant.getRestaurantId())
                .build());

        String token = buildToken(ownerId);

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + restaurant.getRestaurantId() + "/orders/" + orderId + "/reject")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("레스토랑 정보를 찾을 수 없습니다."));

        Optional<OrderApproval> afterOrderApproval = orderApprovalRepository.findByOrderId(orderId);
        assertThat(afterOrderApproval).isPresent();
        assertThat(afterOrderApproval.get().getStatus()).isEqualTo(OrderApprovalStatus.ACCEPTED);
    }

    @DisplayName("레스토랑 소유자가 존재하지 않으면 주문 거절에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenRestaurantOwnerDoesNotExist() throws Exception {
        //given
        String token = buildToken(UUID.randomUUID());

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + restaurantId + "/orders/" + orderId + "/reject")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("레스토랑 오너 정보를 찾을 수 없습니다."));

        Optional<OrderApproval> afterOrderApproval = orderApprovalRepository.findByOrderId(orderId);
        assertThat(afterOrderApproval).isPresent();
        assertThat(afterOrderApproval.get().getStatus()).isEqualTo(OrderApprovalStatus.ACCEPTED);
    }

    @DisplayName("레스토랑이 관리자 승인 대기, 영업 전, 일시 휴업, 완전 폐업, 제재로 영업 정지된 상태라면 주문 거절에 실패하고, 400을 반환한다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("provideRestaurantStatusesThatDisallowOrderRejection")
    void shouldReturn404_whenRestaurantStatusIsNotEligibleForOrderRejection(String status,
                                                                            RestaurantStatus restaurantStatus)
            throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .name("레스토랑")
                .status(restaurantStatus)
                .build();

        restaurantRepository.save(restaurant);

        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .ownerId(ownerId)
                .restaurantId(restaurant.getRestaurantId())
                .build());

        String token = buildToken(ownerId);

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + restaurant.getRestaurantId() + "/orders/" + orderId + "/reject")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("레스토랑이 주문을 받을 수 없는 상태입니다."));

        Optional<OrderApproval> afterOrderApproval = orderApprovalRepository.findByOrderId(orderId);
        assertThat(afterOrderApproval).isPresent();
        assertThat(afterOrderApproval.get().getStatus()).isEqualTo(OrderApprovalStatus.ACCEPTED);
    }

    private static Stream<Arguments> provideRestaurantStatusesThatDisallowOrderRejection() {
        return Stream.of(
                Arguments.of("관리자 승인 대기", RestaurantStatus.PENDING_APPROVAL),
                Arguments.of("영업 전", RestaurantStatus.PRE_OPEN),
                Arguments.of("일시 휴업", RestaurantStatus.TEMP_CLOSED),
                Arguments.of("완전 폐업", RestaurantStatus.PERM_CLOSED),
                Arguments.of("영업 정지", RestaurantStatus.SUSPENDED)
        );
    }

    @DisplayName("주문 내역이 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenRestaurantOwnerDoesNotEx1ist() throws Exception {
        //given
        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .ownerId(ownerId)
                .restaurantId(restaurantId)
                .build());

        String token = buildToken(ownerId);

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + restaurantId + "/orders/" + UUID.randomUUID() + "/reject")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("주문 내역을 찾을 수 없습니다."));
    }

    @DisplayName("주문 상태가 DECLINED, APPROVED, REJECTED 상태라면 주문 거절에 실패하고  400을 반환한다.")
    @ParameterizedTest(name = "[{index}] 주문 상태 : {0}")
    @MethodSource("provideFinalOrderStatuses")
    void shouldReturn400_whenOrderStatusIsNotRejectable(String status, OrderApprovalStatus orderApprovalStatus) throws Exception {
        //given
        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .ownerId(ownerId)
                .restaurantId(restaurantId)
                .build());

        OrderApproval orderApproval = OrderApproval.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .restaurantId(restaurantId)
                .status(orderApprovalStatus)
                .build();

        orderApprovalRepository.save(orderApproval);

        String token = buildToken(ownerId);

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + restaurantId + "/orders/" + orderApproval.getOrderId() + "/reject")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("주문을 승인할 수 없는 상태입니다."));

        Optional<OrderApproval> afterOrderApproval = orderApprovalRepository.findByOrderId(orderApproval.getOrderId());
        assertThat(afterOrderApproval).isPresent();
        assertThat(afterOrderApproval.get().getStatus()).isEqualTo(orderApprovalStatus);
    }

    private static Stream<Arguments> provideFinalOrderStatuses() {
        return Stream.of(
                Arguments.of("DECLINED", OrderApprovalStatus.DECLINED),
                Arguments.of("REJECTED", OrderApprovalStatus.REJECTED),
                Arguments.of("APPROVED", OrderApprovalStatus.APPROVED)
        );
    }

}
