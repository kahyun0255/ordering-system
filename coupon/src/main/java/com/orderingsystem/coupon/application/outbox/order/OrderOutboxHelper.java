package com.orderingsystem.coupon.application.outbox.order;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.coupon.application.outbox.order.model.CouponOrderEventPayload;
import com.orderingsystem.coupon.domain.exception.CouponApplicationException;
import com.orderingsystem.coupon.domain.model.outbox.OrderOutbox;
import com.orderingsystem.coupon.domain.repository.outbox.OrderOutboxRepository;
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
public class OrderOutboxHelper {

    private final OrderOutboxRepository orderOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void save(OrderOutbox orderOutbox) {
        if (!orderOutboxRepository.existsByTypeAndSagaIdAndSagaStatus(ORDER_SAGA_NAME, orderOutbox.getSagaId(),
                orderOutbox.getSagaStatus())) {
            orderOutboxRepository.save(orderOutbox);
            log.info("Order OrderOutbox 저장했습니다. Outbox Id : {}", orderOutbox.getId());
        } else {
            log.warn("이미 저장된 Coupon OrderOutbox가 존재합니다. Saga Id : {}, Type : {}", orderOutbox.getSagaId(),
                    orderOutbox.getType());
        }
    }

    @Transactional
    public void saveOrderOutboxMessage(CouponOrderEventPayload couponOrderEventPayload, SagaStatus sagaStatus, UUID sagaId) {
        save(OrderOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(couponOrderEventPayload.getCreatedAt())
                .type(ORDER_SAGA_NAME)
                .payload(createPayload(couponOrderEventPayload))
                .sagaStatus(sagaStatus)
                .build());
    }

    private String createPayload(CouponOrderEventPayload couponOrderEventPayload) {
        try {
            return objectMapper.writeValueAsString(couponOrderEventPayload);
        } catch (JsonProcessingException e) {
            log.error("CouponOrderEventPayload 생성에 실패했습니다. Order Id : {}", couponOrderEventPayload.getOrderId());
            throw new CouponApplicationException("CouponOrderEventPayload 생성에 실패했습니다. Order Id : "
                    + couponOrderEventPayload.getOrderId());
        }
    }


    @Transactional
    public Optional<OrderOutbox> getCouponOutboxBySagaIdAndSagaStatus(UUID sagaId, SagaStatus... sagaStatus) {
        return orderOutboxRepository.findByTypeAndSagaIdAndSagaStatusIn(ORDER_SAGA_NAME, sagaId,
                Arrays.asList(sagaStatus));
    }

    @Transactional
    public int deleteOlderThan(ZonedDateTime threshold) {
        return orderOutboxRepository.deleteOlderThan(threshold);
    }

    @Transactional(readOnly = true)
    public boolean isProcessed(UUID sagaId) {
        Optional<OrderOutbox> message = orderOutboxRepository.findBySagaIdAndSagaStatus(sagaId, SagaStatus.SUCCEEDED);
        return message.isPresent();
    }

}
