package com.orderingsystem.payment.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.payment.domain.event.PaymentCancelledEvent;
import com.orderingsystem.payment.domain.event.PaymentEvent;
import com.orderingsystem.payment.domain.event.PaymentFailedEvent;
import com.orderingsystem.payment.domain.event.PaymentRefundedEvent;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.Payment;
import com.orderingsystem.payment.domain.model.TransactionType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentValidateAndCancelServiceTest {

    private final PaymentValidateAndCancelService paymentValidateAndCancelService =
            new PaymentValidateAndCancelService();

    @Test
    @DisplayName("결제 취소에 성공하면 결제 상태가 CANCELLED가 되고 결제 금액만큼 CREDIT 이력이 추가된다")
    void shouldCancelPaymentAndAddCreditHistory_whenPaymentIsValid() {
        //given
        Payment payment = completedPaymentOf(new BigDecimal("25.00"));
        List<CreditHistory> creditHistories = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        //when
        PaymentEvent paymentEvent = paymentValidateAndCancelService.validateAndCancel(payment, creditHistories,
                failureMessages);

        //then
        assertThat(paymentEvent).isInstanceOf(PaymentCancelledEvent.class);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(creditHistories).hasSize(1);
        assertThat(creditHistories.get(0).getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(creditHistories.get(0).getAmount()).isEqualTo(new Money(new BigDecimal("25.00")));
    }

    @Test
    @DisplayName("결제 환불에 성공하면 결제 상태가 REFUNDED가 되고 결제 금액만큼 CREDIT 이력이 추가된다")
    void shouldRefundPaymentAndAddCreditHistory_whenPaymentIsValid() {
        //given
        Payment payment = completedPaymentOf(new BigDecimal("25.00"));
        List<CreditHistory> creditHistories = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        //when
        PaymentEvent paymentEvent = paymentValidateAndCancelService.validateAndRefund(payment, creditHistories,
                failureMessages);

        //then
        assertThat(paymentEvent).isInstanceOf(PaymentRefundedEvent.class);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(creditHistories).hasSize(1);
        assertThat(creditHistories.get(0).getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(creditHistories.get(0).getAmount()).isEqualTo(new Money(new BigDecimal("25.00")));
    }

    @Test
    @DisplayName("결제 금액이 0 이하이면 결제 상태가 FAILED가 되고 CREDIT 이력이 추가되지 않는다")
    void shouldFailPaymentAndNotAddCreditHistory_whenPriceIsNotPositive() {
        //given
        Payment payment = completedPaymentOf(new BigDecimal("-25.00"));
        List<CreditHistory> creditHistories = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        //when
        PaymentEvent paymentEvent = paymentValidateAndCancelService.validateAndCancel(payment, creditHistories,
                failureMessages);

        //then
        assertThat(paymentEvent).isInstanceOf(PaymentFailedEvent.class);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(creditHistories).isEmpty();
        assertThat(failureMessages).containsExactly("총 가격은 0보다 커야합니다.");
    }

    private Payment completedPaymentOf(BigDecimal price) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .price(new Money(price))
                .status(PaymentStatus.COMPLETED)
                .build();
    }

}
