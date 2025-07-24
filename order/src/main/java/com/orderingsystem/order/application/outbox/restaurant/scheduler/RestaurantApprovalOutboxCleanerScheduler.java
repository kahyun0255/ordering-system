package com.orderingsystem.order.application.outbox.restaurant.scheduler;

import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.outbox.restaurant.RestaurantApprovalOutboxHelper;
import com.orderingsystem.order.domain.model.outbox.RestaurantApprovalOutbox;
import com.orderingsystem.outbox.OutboxScheduler;
import com.orderingsystem.outbox.OutboxStatus;
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
public class RestaurantApprovalOutboxCleanerScheduler implements OutboxScheduler {

    private final RestaurantApprovalOutboxHelper restaurantApprovalOutboxHelper;

    @Override
    @Scheduled(cron = "@midnight")
    public void processOutboxMessage() {
        Optional<List<RestaurantApprovalOutbox>> outboxMessageResponse =
                restaurantApprovalOutboxHelper.getRestaurantApprovalOutboxMessagesByOutboxStatusAndOutboxSagaStatus(
                        OutboxStatus.COMPLETED,
                        SagaStatus.SUCCEEDED,
                        SagaStatus.FAILED,
                        SagaStatus.COMPENSATED);

        if (outboxMessageResponse.isPresent()) {
            List<RestaurantApprovalOutbox> outboxMessages = outboxMessageResponse.get();

            restaurantApprovalOutboxHelper.deleteAllRestaurantApprovalOutboxMessageByOutboxStatusAndSagaStatus(
                    OutboxStatus.COMPLETED,
                    SagaStatus.SUCCEEDED,
                    SagaStatus.FAILED,
                    SagaStatus.COMPENSATED);

            log.info("{}개의 Order RestaurantApprovalOutbox Message를 삭제했습니다. payloads : {}", outboxMessages.size(),
                    outboxMessages.stream().map(RestaurantApprovalOutbox::getPayload)
                            .collect(Collectors.joining("\n")));
        }
    }
}
