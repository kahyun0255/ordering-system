package com.orderingsystem.restaurant.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.restaurant.application.dto.response.OrderResponse;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import com.orderingsystem.restaurant.domain.repository.OrderApprovalRepository;
import java.time.Instant;
import java.util.List;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class OrderControllerGetTest extends ControllerTestSupport {

    @Autowired
    private OrderApprovalRepository orderApprovalRepository;

    private final UUID restaurantId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();

    OrderApproval orderApprovalApproved = OrderApproval.builder()
            .id(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .restaurantId(restaurantId)
            .status(OrderApprovalStatus.APPROVED)
            .build();
    OrderApproval orderApprovalDeclined = OrderApproval.builder()
            .id(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .restaurantId(restaurantId)
            .status(OrderApprovalStatus.DECLINED)
            .build();
    OrderApproval orderApprovalRejected = OrderApproval.builder()
            .id(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .restaurantId(restaurantId)
            .status(OrderApprovalStatus.REJECTED)
            .build();
    OrderApproval orderApprovalAccepted = OrderApproval.builder()
            .id(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .restaurantId(restaurantId)
            .status(OrderApprovalStatus.ACCEPTED)
            .build();

    OrderApproval orderApproval1 = OrderApproval.builder()
            .id(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .restaurantId(UUID.randomUUID())
            .status(OrderApprovalStatus.ACCEPTED)
            .build();

    OrderApproval orderApproval2 = OrderApproval.builder()
            .id(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .restaurantId(UUID.randomUUID())
            .status(OrderApprovalStatus.ACCEPTED)
            .build();

    @BeforeEach
    void setUp() {
        orderApprovalRepository.saveAll(
                List.of(orderApprovalApproved, orderApprovalDeclined, orderApprovalRejected, orderApprovalAccepted,
                        orderApproval1, orderApproval2));

        ownerRepository.save(Owner.builder()
                .name("소유자")
                .userId(ownerId)
                .build());
    }

    @AfterEach
    @Override
    void tearDown() {
        super.tearDown();
        orderApprovalRepository.deleteAllInBatch();
    }

    @DisplayName("영업 중, 영업 전, 일시 휴업 상태의 레스토랑의 소유자일 경우 레스토랑의 주문 내역을 확인 가능하다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("provideValidRestaurantStatusesForOrderAccess")
    void shouldReturnOrderList_whenOwnerAndRestaurantStatusIsAccessible(String status,
                                                                        RestaurantStatus restaurantStatus)
            throws Exception {
        //given
        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .status(restaurantStatus)
                .name("레스토랑")
                .build());

        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .restaurantId(restaurantId)
                .ownerId(ownerId)
                .build());

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/restaurants/" + restaurantId + "/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(json);
        JsonNode jsonNode = root.get("content");

        List<OrderResponse> response = objectMapper.readValue(jsonNode.toString(),
                new TypeReference<List<OrderResponse>>() {
                });

        assertThat(response).hasSize(4)
                .extracting("id", "orderId", "status")
                .containsExactlyInAnyOrder(
                        tuple(orderApprovalAccepted.getId(), orderApprovalAccepted.getOrderId(),
                                orderApprovalAccepted.getStatus()),
                        tuple(orderApprovalDeclined.getId(), orderApprovalDeclined.getOrderId(),
                                orderApprovalDeclined.getStatus()),
                        tuple(orderApprovalRejected.getId(), orderApprovalRejected.getOrderId(),
                                orderApprovalRejected.getStatus()),
                        tuple(orderApprovalApproved.getId(), orderApprovalApproved.getOrderId(),
                                orderApprovalApproved.getStatus())
                );

        assertThat(response)
                .allSatisfy(r -> assertThat(r.getCreatedAt()).isNotNull());

        int pageNumber = root.get("number").asInt();
        int pageSize = root.get("size").asInt();
        long totalElements = root.get("totalElements").asLong();

        assertThat(pageNumber).isEqualTo(0);
        assertThat(pageSize).isEqualTo(10);
        assertThat(totalElements).isEqualTo(4);
    }

    private static Stream<Arguments> provideValidRestaurantStatusesForOrderAccess() {
        return Stream.of(
                Arguments.of("영업 중", RestaurantStatus.ACTIVE),
                Arguments.of("영업 전", RestaurantStatus.PRE_OPEN),
                Arguments.of("일시 휴업", RestaurantStatus.TEMP_CLOSED)
        );
    }

    @DisplayName("특정 상태의 주문 정보만 조회할 수 있다.")
    @Test
    void shouldReturnOrdersWithGivenStatus() throws Exception {
        //given
        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .status(RestaurantStatus.ACTIVE)
                .name("레스토랑")
                .build());

        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .restaurantId(restaurantId)
                .ownerId(ownerId)
                .build());

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/restaurants/" + restaurantId + "/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .param("status", OrderApprovalStatus.APPROVED.name()))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(json);
        JsonNode jsonNode = root.get("content");

        List<OrderResponse> response = objectMapper.readValue(jsonNode.toString(),
                new TypeReference<List<OrderResponse>>() {
                });

        assertThat(response).hasSize(1)
                .extracting("id", "orderId", "status")
                .containsExactlyInAnyOrder(
                        tuple(orderApprovalApproved.getId(), orderApprovalApproved.getOrderId(),
                                orderApprovalApproved.getStatus())
                );

        assertThat(response)
                .allSatisfy(r -> assertThat(r.getCreatedAt()).isNotNull());

        int pageNumber = root.get("number").asInt();
        int pageSize = root.get("size").asInt();
        long totalElements = root.get("totalElements").asLong();

        assertThat(pageNumber).isEqualTo(0);
        assertThat(pageSize).isEqualTo(10);
        assertThat(totalElements).isEqualTo(1);
    }

    @DisplayName("관리자 승인 대기, 폐업, 영업 정지 상태의 레스토랑일 경우 레스토랑의 주문 내역 확인이 불가능하다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("provideInvalidRestaurantStatusesForOrderAccess")
    void shouldNotReturnOrderList_whenRestaurantStatusIsInvalid(String status, RestaurantStatus restaurantStatus)
            throws Exception {
        //given
        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .status(restaurantStatus)
                .name("레스토랑")
                .build());

        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .restaurantId(restaurantId)
                .ownerId(ownerId)
                .build());

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/restaurants/" + restaurantId + "/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("레스토랑이 주문 정보를 조회할 수 없는 상태입니다."));
    }

    private static Stream<Arguments> provideInvalidRestaurantStatusesForOrderAccess() {
        return Stream.of(
                Arguments.of("관리자 승인 대기", RestaurantStatus.PENDING_APPROVAL),
                Arguments.of("폐업", RestaurantStatus.PERM_CLOSED),
                Arguments.of("영업 정지", RestaurantStatus.SUSPENDED)
        );
    }

    @DisplayName("레스토랑의 소유자가 아닐 경우, 레스토랑의 주문 내역 확인이 불가능하다.")
    @Test
    void shouldDenyOrderAccess_whenUserIsNotOwner() throws Exception {
        //given
        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .status(RestaurantStatus.ACTIVE)
                .name("레스토랑")
                .build());

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/restaurants/" + restaurantId + "/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("주문 정보를 확인할 권한이 없습니다."));
    }

    @DisplayName("레스토랑이 존재하지 않거나 삭제되었을 경우, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("provideMissingOrDeletedRestaurants")
    void shouldThrowNotFoundException_whenRestaurantIsMissingOrDeleted(String status, RestaurantStatus restaurantStatus)
            throws Exception {
        //given
        if (restaurantStatus != null) {
            restaurantRepository.save(Restaurant.builder()
                    .restaurantId(restaurantId)
                    .status(restaurantStatus)
                    .name("레스토랑")
                    .build());
        }

        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .restaurantId(restaurantId)
                .ownerId(ownerId)
                .build());

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/restaurants/" + restaurantId + "/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("레스토랑 정보를 찾을 수 없습니다."));
    }

    private static Stream<Arguments> provideMissingOrDeletedRestaurants() {
        return Stream.of(
                Arguments.of("삭제됨", RestaurantStatus.DELETED),
                Arguments.of("존재하지 않음", null)
        );
    }

    @DisplayName("존재하지 않는 주문 상태로 조회를 요청하면 레스토랑의 주문 내역 확인이 불가능하다.")
    @Test
    void shouldThrowException_whenOrderStatusIsInvalid() throws Exception {
        //given
        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .status(RestaurantStatus.ACTIVE)
                .name("레스토랑")
                .build());

        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .restaurantId(restaurantId)
                .ownerId(ownerId)
                .build());

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/restaurants/" + restaurantId + "/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .param("status", "invalidStatus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 주문 상태입니다."));
    }

}
