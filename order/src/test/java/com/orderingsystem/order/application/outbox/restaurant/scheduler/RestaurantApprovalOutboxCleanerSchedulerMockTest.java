package com.orderingsystem.order.application.outbox.restaurant.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.outbox.restaurant.RestaurantApprovalOutboxHelper;
import com.orderingsystem.outbox.OutboxStatus;
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
class RestaurantApprovalOutboxCleanerSchedulerMockTest {

    @InjectMocks
    private RestaurantApprovalOutboxCleanerScheduler restaurantApprovalOutboxCleanerScheduler;

    @Mock
    private RestaurantApprovalOutboxHelper restaurantApprovalOutboxHelper;

    @DisplayName("삭제 할 메시지가 없으면 삭제 메서드는 호출되지 않는다.")
    @Test
    void doNotion_whenNoDeleteMessages() {
        // given
        when(restaurantApprovalOutboxHelper.getRestaurantApprovalOutboxMessagesByOutboxStatusAndOutboxSagaStatus(
                OutboxStatus.COMPLETED,
                SagaStatus.SUCCEEDED,
                SagaStatus.FAILED,
                SagaStatus.COMPENSATED
        )).thenReturn(Optional.empty());

        // when
        restaurantApprovalOutboxCleanerScheduler.processOutboxMessage();

        // then
        verify(restaurantApprovalOutboxHelper, never()).deleteAllRestaurantApprovalOutboxMessageByOutboxStatusAndSagaStatus(
                any(), any(), any(), any());
    }

}
