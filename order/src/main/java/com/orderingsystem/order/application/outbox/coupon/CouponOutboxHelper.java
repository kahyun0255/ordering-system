package com.orderingsystem.order.application.outbox.coupon;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.outbox.coupon.model.OrderCouponEventPayload;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import com.orderingsystem.order.domain.model.outbox.CouponOutbox;
import com.orderingsystem.order.domain.repository.outbox.CouponOutboxRepository;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponOutboxHelper {

    private final CouponOutboxRepository couponOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void save(CouponOutbox couponOutbox) {
        if (!couponOutboxRepository.existsByTypeAndSagaIdAndSagaStatus(ORDER_SAGA_NAME,
                couponOutbox.getSagaId(), couponOutbox.getSagaStatus())) {
            couponOutboxRepository.save(couponOutbox);
            log.info("Order CouponOutbox 저장했습니다. Outbox Id : {}", couponOutbox.getId());
        } else {
            log.warn("이미 저장된 Order CouponOutbox가 존재합니다. Saga Id : {}, Type : {} OrderStatus : {}",
                    couponOutbox.getSagaId(), couponOutbox.getType(), couponOutbox.getOrderStatus());
        }
    }

    @Transactional
    public void saveCouponOutboxMessage(OrderCouponEventPayload orderCouponEventPayload, OrderStatus orderStatus,
                                        SagaStatus sagaStatus, UUID sagaId) {
        save(CouponOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(orderCouponEventPayload.getCreatedAt())
                .type(ORDER_SAGA_NAME)
                .payload(createPayload(orderCouponEventPayload))
                .orderStatus(orderStatus)
                .sagaStatus(sagaStatus)
                .build());
    }

    private String createPayload(OrderCouponEventPayload orderCouponEventPayload) {
        try {
            return objectMapper.writeValueAsString(orderCouponEventPayload);
        } catch (JsonProcessingException e) {
            log.error("OrderCouponEventPayload 생성에 실패했습니다. Order Id : {}", orderCouponEventPayload.getOrderId());
            throw new OrderDomainException(
                    "OrderCouponEventPayload 생성에 실패했습니다. Order Id : " + orderCouponEventPayload.getOrderId());
        }
    }


    @Transactional
    public Optional<CouponOutbox> getCouponOutboxBySagaIdAndSagaStatus(UUID sagaId, SagaStatus... sagaStatus) {
        return couponOutboxRepository.findByTypeAndSagaIdAndSagaStatusIn(ORDER_SAGA_NAME, sagaId,
                Arrays.asList(sagaStatus));
    }

    @Transactional
    public int deleteOlderThan(ZonedDateTime threshold) {
        return couponOutboxRepository.deleteOlderThan(threshold);
    }

    @Transactional(readOnly = true)
    public boolean isCouponProcessed(UUID sagaId) {
        Optional<CouponOutbox> message = couponOutboxRepository.findBySagaIdAndSagaStatus(sagaId, SagaStatus.SUCCEEDED);
        return message.isPresent();
    }

}
