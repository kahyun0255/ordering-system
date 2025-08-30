package com.orderingsystem.application.outbox.scheduler;

import com.orderingsystem.application.outbox.UserOutboxHelper;
import com.orderingsystem.outbox.OutboxScheduler;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserOutboxCleanerScheduler implements OutboxScheduler {

    private final UserOutboxHelper userOutboxHelper;

    @Value("${outbox.delete-ttl}")
    private Long deleteTtl;

    @Override
    @Scheduled(cron = "@midnight")
    public void processOutboxMessage() {
        ZonedDateTime threshold = ZonedDateTime.now().minusDays(deleteTtl);
        int deleted = userOutboxHelper.deleteOlderThan(threshold);

        log.info("{}개의 User UserOutbox Message 삭제", deleted);
    }
}
