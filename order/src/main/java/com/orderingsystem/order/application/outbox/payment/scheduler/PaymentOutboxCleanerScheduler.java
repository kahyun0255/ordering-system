package com.orderingsystem.order.application.outbox.payment.scheduler;

import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.outbox.payment.PaymentOutboxHelper;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
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
public class PaymentOutboxCleanerScheduler implements OutboxScheduler {

    private final PaymentOutboxHelper paymentOutboxHelper;

    @Override
    @Scheduled(cron = "@midnight")
    public void processOutboxMessage() {
        Optional<List<PaymentOutbox>> outboxMessageResponse =
                paymentOutboxHelper.getPaymentOutboxMessagesByOutboxStatusAndOutboxSagaStatus(
                        OutboxStatus.COMPLETED,
                        SagaStatus.SUCCEEDED,
                        SagaStatus.FAILED,
                        SagaStatus.COMPENSATED);

        if (outboxMessageResponse.isPresent()) {
            List<PaymentOutbox> outboxMessages = outboxMessageResponse.get();

            paymentOutboxHelper.deleteAllPaymentOutboxMessageByOutboxStatusAndSagaStatus(
                    OutboxStatus.COMPLETED,
                    SagaStatus.SUCCEEDED,
                    SagaStatus.FAILED,
                    SagaStatus.COMPENSATED);

            log.info("{}개의 Order PaymentOutbox Message를 삭제했습니다. payloads : {}", outboxMessages.size(),
                    outboxMessages.stream().map(PaymentOutbox::getPayload).collect(Collectors.joining("\n")));
        }
    }
}
