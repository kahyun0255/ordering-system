package com.orderingsystem.restaurant.application.outbox.order.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orderingsystem.restaurant.application.outbox.order.OrderOutboxHelper;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class OrderOutboxCleanerSchedulerMockTest {

    @InjectMocks
    private OrderOutboxCleanerScheduler orderOutboxCleanerScheduler;

    @Mock
    private OrderOutboxHelper orderOutboxHelper;

    @DisplayName("삭제 할 메시지가 없으면 삭제 메서드는 호출되지 않는다.")
    @Test
    void doNotion_whenNoDeleteMessages() {
        // given
        when(orderOutboxHelper.getOrderOutboxMessageByOutboxStatus(
        )).thenReturn(Optional.empty());

        // when
        orderOutboxCleanerScheduler.processOutboxMessage();

        // then
        verify(orderOutboxHelper, never()).deleteAllOrderOutboxByOutboxStatus();
    }

}
