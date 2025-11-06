package com.orderingsystem.restaurant.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import com.orderingsystem.restaurant.domain.repository.OrderApprovalRepository;
import com.orderingsystem.restaurant.domain.repository.outbox.OrderOutboxRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class OrderApprovalControllerTest extends ControllerTestSupport {

    @Autowired
    private OrderApprovalRepository orderApprovalRepository;

    @Autowired
    private OrderOutboxRepository orderOutboxRepository;

    @AfterEach
    @Override
    void tearDown() {
        super.tearDown();
        orderApprovalRepository.deleteAllInBatch();
        orderOutboxRepository.deleteAllInBatch();
    }

    @DisplayName("주문을 요청한 레스토랑의 소유자이고, 주문 상태가 ACCEPTED면 주문 승인이 가능하다.")
    @Test
    void shouldApproveOrder_whenUserIsRestaurantOwner() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        Owner owner = Owner.builder()
                .userId(UUID.randomUUID())
                .name("소유자")
                .build();
        ownerRepository.save(owner);

        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .restaurantId(restaurant.getRestaurantId())
                .ownerId(owner.getUserId())
                .build());

        OrderApproval orderApproval = OrderApproval.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .status(OrderApprovalStatus.ACCEPTED)
                .restaurantId(restaurant.getRestaurantId())
                .build();
        orderApprovalRepository.save(orderApproval);

        String token = buildToken(owner.getUserId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        mockMvc.perform(
                        post("/api/restaurants/" + restaurant.getRestaurantId() + "/orders/" + orderApproval.getOrderId()
                                + "/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        //then
        Optional<OrderApproval> after = orderApprovalRepository.findById(orderApproval.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getStatus()).isEqualTo(OrderApprovalStatus.APPROVED);

        assertThat(orderOutboxRepository.count()).isOne();
    }

    @DisplayName("주문을 요청한 레스토랑의 소유자이고, 주문 상태가 APPROVED, DECLINED, REJECTED면 주문 승인이 불가능하다.")
    @ParameterizedTest(name = "[{index}] 주문 상태 : {0}")
    @MethodSource("dd")
    void shouldNotApproveOrder_whenStatusIsFinalized(String status, OrderApprovalStatus orderApprovalStatus)
            throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        Owner owner = Owner.builder()
                .userId(UUID.randomUUID())
                .name("소유자")
                .build();
        ownerRepository.save(owner);

        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .restaurantId(restaurant.getRestaurantId())
                .ownerId(owner.getUserId())
                .build());

        OrderApproval orderApproval = OrderApproval.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .status(orderApprovalStatus)
                .restaurantId(restaurant.getRestaurantId())
                .build();
        orderApprovalRepository.save(orderApproval);

        String token = buildToken(owner.getUserId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        mockMvc.perform(
                        post("/api/restaurants/" + restaurant.getRestaurantId() + "/orders/" + orderApproval.getOrderId()
                                + "/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("주문을 승인할 수 없는 상태입니다."));

        //then
        Optional<OrderApproval> after = orderApprovalRepository.findById(orderApproval.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getStatus()).isEqualTo(orderApprovalStatus);

        assertThat(orderOutboxRepository.count()).isZero();
    }

    private static Stream<Arguments> dd() {
        return Stream.of(
                Arguments.of("승인됨", OrderApprovalStatus.APPROVED),
                Arguments.of("무시됨", OrderApprovalStatus.DECLINED),
                Arguments.of("거절됨", OrderApprovalStatus.REJECTED)
        );
    }

    @DisplayName("주문을 요청한 레스토랑의 소유자가 아니라면 주문 승인이 불가능하다.")
    @Test
    void shouldNotApproveOrder_whenUserIsNotRestaurantOwner() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        Owner owner = Owner.builder()
                .userId(UUID.randomUUID())
                .name("소유자")
                .build();
        ownerRepository.save(owner);

        OrderApproval orderApproval = OrderApproval.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .status(OrderApprovalStatus.ACCEPTED)
                .restaurantId(restaurant.getRestaurantId())
                .build();
        orderApprovalRepository.save(orderApproval);

        String token = buildToken(owner.getUserId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        mockMvc.perform(
                        post("/api/restaurants/" + restaurant.getRestaurantId() + "/orders/" + orderApproval.getOrderId()
                                + "/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 레스토랑의 주문을 승인할 권한이 없습니다."));

        //then
        Optional<OrderApproval> after = orderApprovalRepository.findById(orderApproval.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getStatus()).isEqualTo(OrderApprovalStatus.ACCEPTED);

        assertThat(orderOutboxRepository.count()).isZero();
    }

    @DisplayName("주문내역이 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldThrowNotFoundException_whenOrderDoesNotExist() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        Owner owner = Owner.builder()
                .userId(UUID.randomUUID())
                .name("소유자")
                .build();
        ownerRepository.save(owner);

        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .restaurantId(restaurant.getRestaurantId())
                .ownerId(owner.getUserId())
                .build());

        String token = buildToken(owner.getUserId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + restaurant.getRestaurantId() + "/orders/" + UUID.randomUUID()
                                + "/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("주문 내역을 찾을 수 없습니다."));

        assertThat(orderOutboxRepository.count()).isZero();
    }


}
