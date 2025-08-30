package com.orderingsystem.application.outbox.scheduler;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.domain.model.outbox.UserOutbox;
import com.orderingsystem.domain.repository.outbox.UserOutboxRepository;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = "outbox.delete-ttl=3")
class UserOutboxCleanerSchedulerTest {

    @Autowired
    private UserOutboxCleanerScheduler userOutboxCleanerScheduler;

    @Autowired
    private UserOutboxRepository userOutboxRepository;

    @DisplayName("Delete TTL 이전에 생성한 Outbox Message를 삭제한다.")
    @Test
    void shouldDeleteOutboxMessagesCreatedBeforeDeleteTTL() {
        //given
        UserOutbox userOutbox1 = getUserOutbox(ZonedDateTime.now().minusDays(4));
        UserOutbox userOutbox2 = getUserOutbox(ZonedDateTime.now().minusDays(3));
        UserOutbox userOutbox3 = getUserOutbox(ZonedDateTime.now().minusDays(3));

        UserOutbox userOutbox4 = getUserOutbox(ZonedDateTime.now().minusDays(1));
        UserOutbox userOutbox5 = getUserOutbox(ZonedDateTime.now().minusDays(2));

        userOutboxRepository.saveAll(
                List.of(userOutbox1, userOutbox2, userOutbox3, userOutbox4, userOutbox5));

        long beforeCount = userOutboxRepository.count();
        assertThat(beforeCount).isEqualTo(5L);

        //when
        userOutboxCleanerScheduler.processOutboxMessage();

        //then
        long afterCount = userOutboxRepository.count();
        assertThat(afterCount).isEqualTo(2L);
    }

    private UserOutbox getUserOutbox(ZonedDateTime createdAt) {
        return UserOutbox.builder()
                .id(UUID.randomUUID())
                .createdAt(createdAt)
                .type(ORDER_SAGA_NAME)
                .payload("payload")
                .build();
    }

}

