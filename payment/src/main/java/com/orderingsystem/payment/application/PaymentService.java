package com.orderingsystem.payment.application;

import com.orderingsystem.payment.application.dto.request.PaymentRequest;
import com.orderingsystem.payment.domain.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRequestHelper paymentRequestHelper;

    public void completePayment(PaymentRequest paymentRequest) {
        PaymentEvent paymentEvent = paymentRequestHelper.persistPayment(paymentRequest);
        log.info("결제 이벤트 발행. Payment Id : {}, Order Id : {}", paymentEvent.getPayment().getId(),
                paymentEvent.getPayment().getOrderId());
        paymentEvent.fire();
    }

    public void cancelPayment(PaymentRequest paymentRequest) {
        PaymentEvent paymentEvent = paymentRequestHelper.persistCancelPayment(paymentRequest);

        log.info("결제 이벤트 발행. Payment Id : {}, Order Id : {}", paymentEvent.getPayment().getId(),
                paymentEvent.getPayment().getOrderId());
        paymentEvent.fire();
    }
}
