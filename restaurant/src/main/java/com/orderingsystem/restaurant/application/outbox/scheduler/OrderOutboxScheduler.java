package com.orderingsystem.restaurant.application.outbox.scheduler;

import com.orderingsystem.outbox.OutboxScheduler;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.application.outbox.OrderOutboxHelper;
import com.orderingsystem.restaurant.application.publisher.RestaurantApprovalResponseMessagePublisher;
import com.orderingsystem.restaurant.domain.model.outbox.OrderOutbox;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderOutboxScheduler implements OutboxScheduler {

    private final OrderOutboxHelper orderOutboxHelper;
    private final RestaurantApprovalResponseMessagePublisher restaurantApprovalResponseMessagePublisher;

    @Override
    @Scheduled(fixedRateString = "${outbox-scheduler.fixed-rate}",
            initialDelayString = "${outbox-scheduler.initial-delay}")
    public void processOutboxMessage() {
        Optional<List<OrderOutbox>> outboxMessagesResponse = orderOutboxHelper.getOrderOutboxMessageByOutboxStatus(
                OutboxStatus.STARTED);

        if (outboxMessagesResponse.isPresent() && !outboxMessagesResponse.get().isEmpty()){
            List<OrderOutbox> outboxMessages = outboxMessagesResponse.get();

            log.info("{}개의 Restaurant OrderOutbox Message를 수신했으며, 해당 ID {}들을 메시지 버스로 전송합니다.",
                    outboxMessages.size(), outboxMessages.stream().map(outboxMessage ->
                            outboxMessage.getId().toString()).collect(Collectors.joining(", ")));

            outboxMessages.forEach(outboxMessage ->
                    restaurantApprovalResponseMessagePublisher.publish(outboxMessage, this::updateOutboxStatus));

            log.info("{}개의 Restaurant OrderOutbox Message를 메시지 버스로 전송했습니다.", outboxMessages.size());
        }
    }

    private void updateOutboxStatus(OrderOutbox orderOutbox, OutboxStatus outboxStatus) {
        orderOutbox.updateOutboxStatus(outboxStatus);
        orderOutboxHelper.save(orderOutbox);
        log.info("Restaurant OrderOutbox 메시지의 OutboxStatus를 {}로 업데이트 했습니다.", outboxStatus.name());
    }
}
