package com.orderingsystem.order.application;

import static com.orderingsystem.common.domain.status.OrderStatus.CANCELLING;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.common.saga.SagaStep;
import com.orderingsystem.order.application.dto.response.CouponResponse;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.outbox.coupon.CouponOutboxHelper;
import com.orderingsystem.order.application.outbox.restaurant.RestaurantAcceptOutboxHelper;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.outbox.CouponOutbox;
import com.orderingsystem.order.domain.model.outbox.MessageType;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.repository.outbox.ProcessedMessageRepository;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCouponService implements SagaStep<CouponResponse> {

    private final CouponOutboxHelper couponOutboxHelper;
    private final OrderRepository orderRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final RestaurantAcceptOutboxHelper restaurantAcceptOutboxHelper;
    private final OrderDataMapper orderDataMapper;

    @Override
    @Transactional
    public void process(CouponResponse couponResponse) {
        UUID orderId = couponResponse.getOrderId();
        UUID sagaId = couponResponse.getSagaId();

        log.info("해당 주문의 쿠폰 완료 처리를 시작합니다. Order Id : {}", orderId);

        Optional<CouponOutbox> couponOutboxMessageResponse = couponOutboxHelper.getCouponOutboxBySagaIdAndSagaStatus(
                sagaId,
                SagaStatus.STARTED);

        if (couponOutboxMessageResponse.isEmpty()) {
            log.info("STARTED 상태의 CouponOutbox 없음. SagaId: {}", sagaId);
            return;
        }

        CouponOutbox couponOutboxMessage = couponOutboxMessageResponse.get();

        Order order = findOrder(orderId);

        if (order.getOrderStatus() == OrderStatus.APPROVED
                || order.getOrderStatus() == OrderStatus.CANCELLED
                || order.getOrderStatus() == CANCELLING) {
            log.info("주문이 이미 승인/취소 처리된 상태입니다. 결제 처리를 생략합니다. Order Id : {}", orderId);
            return;
        }

        if (checkAndMarkProcessed(couponResponse, MessageType.COUPON_COMPLETE)) {
            return;
        }

        updateCouponOutboxMessage(couponOutboxMessage, SagaStatus.SUCCEEDED, order.getOrderStatus());

        if (order.getOrderStatus().equals(OrderStatus.PAID)) {
            log.info("주문 결제 및 쿠폰 완료. OrderId : [{}], SagaId : [{}]", orderId, sagaId);

            SagaStatus sagaStatus =
                    OrderStatusToSagaStatus.orderStatusToSagaStatus(order.getOrderStatus());

            restaurantAcceptOutboxHelper.saveRestaurantAcceptOutboxMessage(
                    orderDataMapper.orderToRestaurantAcceptEventPayload(order, sagaId),
                    order.getOrderStatus(),
                    sagaStatus,
                    sagaId
            );
        } else {
            log.info("쿠폰 처리는 완료되었으나 결제 대기중. OrderId : [{}], SagaId : [{}]", orderId, sagaId);
        }

    }

    @Override
    public void rollback(CouponResponse couponResponse) {
        //TODO : 쿠폰 롤백 처리
    }

    private Order findOrder(UUID orderId) {
        Optional<Order> order = orderRepository.findById(orderId);

        if (order.isEmpty()) {
            log.error("주문을 찾을 수 없습니다. Order Id : {}", orderId);
            throw new OrderNotFoundException("주문을 찾을 수 없습니다. Order Id : " + orderId);
        }
        return order.get();
    }

    private boolean checkAndMarkProcessed(CouponResponse couponResponse, MessageType messageType) {
        int inserted = processedMessageRepository.insertIgnore(
                couponResponse.getId(),
                messageType.name(),
                ZonedDateTime.now()
        );

        if (inserted == 0) {
            log.info("이미 {} 메시지가 처리되었습니다. Order Id : {}, Saga Id : {}",
                    messageType, couponResponse.getOrderId(), couponResponse.getSagaId());
            return true;
        }
        return false;
    }

    private void updateCouponOutboxMessage(CouponOutbox couponOutboxMessage, SagaStatus sagaStatus, OrderStatus orderStatus) {
        couponOutboxMessage.updateSagaStatus(sagaStatus);
        couponOutboxMessage.updateProcessedAt(ZonedDateTime.now());
        couponOutboxMessage.updateOrderStatus(orderStatus);
    }

}
