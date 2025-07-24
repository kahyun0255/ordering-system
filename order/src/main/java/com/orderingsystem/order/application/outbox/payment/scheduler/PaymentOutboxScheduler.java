package com.orderingsystem.order.application.outbox.payment.scheduler;

import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.outbox.payment.PaymentOutboxHelper;
import com.orderingsystem.order.application.publisher.PaymentRequestMessagePublisher;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.outbox.OutboxScheduler;
import com.orderingsystem.outbox.OutboxStatus;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxScheduler implements OutboxScheduler {

    private final PaymentOutboxHelper paymentOutboxHelper;
    private final PaymentRequestMessagePublisher paymentRequestMessagePublisher;

    @Override
    @Scheduled(fixedDelayString = "${outbox-scheduler.fixed-rate}",
            initialDelayString = "${outbox-scheduler.initial-delay}")
    public void processOutboxMessage() {
        Optional<List<PaymentOutbox>> outboxMessageResponse =
                paymentOutboxHelper.getPaymentOutboxMessagesByOutboxStatusAndOutboxSagaStatus(
                        OutboxStatus.STARTED, SagaStatus.STARTED, SagaStatus.COMPENSATING);

        if (outboxMessageResponse.isPresent() && !outboxMessageResponse.get().isEmpty()){
            List<PaymentOutbox> outboxMessages = outboxMessageResponse.get();

            log.info("{}개의 Order PaymentOutbox Message를 수신했으며, 해당 ID {}들을 메시지 버스로 전송합니다.",
                    outboxMessages.size(), outboxMessages.stream().map(outboxMessage ->
                            outboxMessage.getId().toString()).collect(Collectors.joining(", ")));

            outboxMessages.forEach(outboxMessage ->
                    paymentRequestMessagePublisher.publish(outboxMessage, this::updateOutboxStatus));

            log.info("{}개의 Order PaymentOutbox Message를 메시지 버스로 전송했습니다.", outboxMessages.size());
        }
    }

    public void updateOutboxStatus(PaymentOutbox paymentOutbox, OutboxStatus outboxStatus) {
        paymentOutbox.updateOutboxStatus(outboxStatus);
        paymentOutbox.updateProcessedAt(ZonedDateTime.now());
        paymentOutboxHelper.save(paymentOutbox);
        log.info("Order PaymentOutbox 메시지의 OutboxStatus를 {}로 업데이트 했습니다.", outboxStatus.name());
    }
}
