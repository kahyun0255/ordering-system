package com.orderingsystem.order.application.outbox.restaurant.scheduler;

import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.outbox.restaurant.RestaurantApprovalOutboxHelper;
import com.orderingsystem.order.application.publisher.RestaurantApprovalRequestMessagePublisher;
import com.orderingsystem.order.domain.model.outbox.RestaurantApprovalOutbox;
import com.orderingsystem.outbox.OutboxScheduler;
import com.orderingsystem.outbox.OutboxStatus;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class RestaurantApprovalOutboxScheduler implements OutboxScheduler {

    private final RestaurantApprovalOutboxHelper restaurantApprovalOutboxHelper;
    private final RestaurantApprovalRequestMessagePublisher restaurantApprovalRequestMessagePublisher;

    @Override
    @Scheduled(fixedDelayString = "${outbox-scheduler.fixed-rate}",
            initialDelayString = "${outbox-scheduler.initial-delay}")
    public void processOutboxMessage() {
        Optional<List<RestaurantApprovalOutbox>> outboxMessagesResponse =
                restaurantApprovalOutboxHelper.getApprovalOutboxMessagesByOutboxStatusAndSagaStatus(
                        OutboxStatus.STARTED,
                        SagaStatus.PROCESSING);

        if (outboxMessagesResponse.isPresent() && !outboxMessagesResponse.get().isEmpty()){
            List<RestaurantApprovalOutbox> outboxMessages = outboxMessagesResponse.get();

            log.info("{}개의 Order RestaurantApprovalOutbox Message를 수신했으며, 해당 ID {}들을 메시지 버스로 전송합니다.",
                    outboxMessages.size(), outboxMessages.stream().map(outboxMessage ->
                            outboxMessage.getId().toString()).collect(Collectors.joining(", ")));

            outboxMessages.forEach(outboxMessage ->
                    restaurantApprovalRequestMessagePublisher.publish(outboxMessage, this::updateOutboxStatus));

            log.info("{}개의 Order PaymentOutbox Message를 메시지 버스로 전송했습니다.", outboxMessages.size());
        }
    }

    public void updateOutboxStatus(RestaurantApprovalOutbox restaurantApprovalOutbox, OutboxStatus outboxStatus) {
        restaurantApprovalOutbox.updateOutboxStatus(outboxStatus);
        restaurantApprovalOutbox.updateProcessedAt(ZonedDateTime.now());
        restaurantApprovalOutboxHelper.save(restaurantApprovalOutbox);
        log.info("Order RestaurantApprovalOutbox 메시지의 OutboxStatus를 {}로 업데이트 했습니다.", outboxStatus.name());
    }
}
