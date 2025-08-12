package com.orderingsystem.restaurant.application.outbox.restaruantupdate.scheduler;

import com.orderingsystem.outbox.OutboxScheduler;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.application.outbox.restaruantupdate.RestaurantUpdateOutboxHelper;
import com.orderingsystem.restaurant.domain.model.outbox.RestaurantUpdateOutbox;
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
public class RestaurantUpdateOutboxCleanerScheduler implements OutboxScheduler {

    private final RestaurantUpdateOutboxHelper orderOutboxHelper;

    @Override
    @Scheduled(cron = "@midnight")
    @Transactional
    public void processOutboxMessage() {
        Optional<List<RestaurantUpdateOutbox>> outboxMessageResponse =
                orderOutboxHelper.getOrderOutboxMessageByOutboxStatus(OutboxStatus.COMPLETED);

        if (outboxMessageResponse.isPresent() && !outboxMessageResponse.get().isEmpty()) {
            List<RestaurantUpdateOutbox> outboxMessages = outboxMessageResponse.get();

            orderOutboxHelper.deleteAllOrderOutboxByOutboxStatus(OutboxStatus.COMPLETED);

            log.info("{}개의 Order RestaurantUpdateOutbox Message를 삭제했습니다. payloads : {}", outboxMessages.size(),
                    outboxMessages.stream().map(RestaurantUpdateOutbox::getPayload).collect(Collectors.joining("\n")));
        }
    }
}
