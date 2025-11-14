package com.orderingsystem.order.application.outbox.restaurant.scheduler;

import com.orderingsystem.order.application.outbox.restaurant.RestaurantAcceptOutboxHelper;
import com.orderingsystem.outbox.OutboxScheduler;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RestaurantAcceptOutboxCleanerScheduler implements OutboxScheduler {

    private final RestaurantAcceptOutboxHelper restaurantAcceptOutboxHelper;

    @Value("${outbox.delete-ttl}")
    private Long deleteTtl;

    @Override
    @Scheduled(cron = "@midnight")
    public void processOutboxMessage() {
        ZonedDateTime threshold = ZonedDateTime.now().minusDays(deleteTtl);
        int deleted = restaurantAcceptOutboxHelper.deleteOlderThan(threshold);

        log.info("{}개의 Order RestaurantApprovalOutbox Message 삭제", deleted);
    }
}
