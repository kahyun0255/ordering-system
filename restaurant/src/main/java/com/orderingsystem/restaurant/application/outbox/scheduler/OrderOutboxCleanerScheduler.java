package com.orderingsystem.restaurant.application.outbox.scheduler;

import com.orderingsystem.outbox.OutboxScheduler;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.application.outbox.OrderOutboxHelper;
import com.orderingsystem.restaurant.domain.model.outbox.OrderOutbox;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxCleanerScheduler implements OutboxScheduler {

    private final OrderOutboxHelper orderOutboxHelper;

    @Override
    @Scheduled(cron = "@midnight")
    @Transactional
    public void processOutboxMessage() {
        Optional<List<OrderOutbox>> outboxMessageResponse =
                orderOutboxHelper.getOrderOutboxMessageByOutboxStatus(OutboxStatus.COMPLETED);

        if (outboxMessageResponse.isPresent() && !outboxMessageResponse.get().isEmpty()) {
            List<OrderOutbox> outboxMessages = outboxMessageResponse.get();

            orderOutboxHelper.deleteAllOrderOutboxByOutboxStatus(OutboxStatus.COMPLETED);

            log.info("{}개의 Order RestaurantApprovalOutbox Message를 삭제했습니다. payloads : {}", outboxMessages.size(),
                    outboxMessages.stream().map(OrderOutbox::getPayload).collect(Collectors.joining("\n")));
        }
    }
}
