package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.restaurant.application.dto.response.OrderResponse;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import com.orderingsystem.restaurant.domain.repository.OrderApprovalRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderApprovalRepository orderApprovalRepository;

    @InjectMocks
    private OrderService orderService;

    private final UUID restaurantId = UUID.randomUUID();

    @DisplayName("status가 null이거나 공백이면 레스토랑의 모든 주문을 페이징 조회한다.")
    @Test
    void shouldReturnAllPagedOrders_whenStatusIsNullOrBlank() {
        //given
        PageRequest pageRequest = PageRequest.of(0, 10);

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

        Page<OrderApproval> page = new PageImpl<>(List.of(orderApprovalApproved, orderApprovalDeclined), pageRequest,
                2);

        given(orderApprovalRepository.findByRestaurantId(restaurantId, pageRequest)).willReturn(page);

        //when
        Page<OrderResponse> result = orderService.findOrders(restaurantId, pageRequest, null);

        //then
        assertThat(result.getContent()).hasSize(2)
                .extracting("id", "orderId", "status")
                .containsExactlyInAnyOrder(
                        tuple(orderApprovalApproved.getId(), orderApprovalApproved.getOrderId(),
                                orderApprovalApproved.getStatus()),
                        tuple(orderApprovalDeclined.getId(), orderApprovalDeclined.getOrderId(),
                                orderApprovalDeclined.getStatus())
                );

        verify(orderApprovalRepository).findByRestaurantId(restaurantId, pageRequest);
    }

    @DisplayName("status가 유효한 값이면 해당 상태로 필터링해 주문을 페이징 조회한다.")
    @Test
    void shouldFilterAndPageOrders_whenStatusIsValid() {
        //given
        PageRequest pageRequest = PageRequest.of(0, 10);
        String status = OrderApprovalStatus.APPROVED.name();

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

        Page<OrderApproval> page = new PageImpl<>(List.of(orderApprovalApproved), pageRequest, 1);

        given(orderApprovalRepository.findByRestaurantIdAndStatus(restaurantId, OrderApprovalStatus.APPROVED,
                pageRequest)).willReturn(page);

        //when
        Page<OrderResponse> result = orderService.findOrders(restaurantId, pageRequest, status);

        //then
        assertThat(result.getContent()).hasSize(1)
                .extracting("id", "orderId", "status")
                .containsExactlyInAnyOrder(
                        tuple(orderApprovalApproved.getId(), orderApprovalApproved.getOrderId(),
                                orderApprovalApproved.getStatus())
                );

        verify(orderApprovalRepository).findByRestaurantIdAndStatus(restaurantId, OrderApprovalStatus.APPROVED,
                pageRequest);
    }

    @DisplayName("status가 유효한 값이면 소문자라도 해당 상태로 필터링해 주문을 페이징 조회한다.")
    @Test
    void shouldFilterOrdersByStatus_whenStatusIsLowercase() {
        //given
        PageRequest pageRequest = PageRequest.of(0, 10);
        String status = "approved";

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

        Page<OrderApproval> page = new PageImpl<>(List.of(orderApprovalApproved), pageRequest, 1);

        given(orderApprovalRepository.findByRestaurantIdAndStatus(restaurantId, OrderApprovalStatus.APPROVED,
                pageRequest)).willReturn(page);

        //when
        Page<OrderResponse> result = orderService.findOrders(restaurantId, pageRequest, status);

        //then
        assertThat(result.getContent()).hasSize(1)
                .extracting("id", "orderId", "status")
                .containsExactlyInAnyOrder(
                        tuple(orderApprovalApproved.getId(), orderApprovalApproved.getOrderId(),
                                orderApprovalApproved.getStatus())
                );

        verify(orderApprovalRepository).findByRestaurantIdAndStatus(restaurantId, OrderApprovalStatus.APPROVED,
                pageRequest);
    }

    @DisplayName("status가 유효하지 않은 값이면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenOrderStatusIsInvalid() {
        //given
        PageRequest pageRequest = PageRequest.of(0, 10);
        String invalidStatus = "InvalidStatus";

        //when, then
        assertThatThrownBy(() -> orderService.findOrders(restaurantId, pageRequest, invalidStatus))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지원하지 않는 주문 상태입니다.");
    }

}
