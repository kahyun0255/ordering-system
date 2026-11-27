package com.orderingsystem.payment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PaymentTest {

    @Test
    @DisplayName("결제 상태가 COMPLETED이면 취소할 수 있다")
    void shouldReturnTrue_whenPaymentStatusIsCompleted() {
        //given
        Payment payment = paymentOf(PaymentStatus.COMPLETED);

        //when
        boolean cancellable = payment.isCancellable();

        // then
        assertThat(cancellable).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"FAILED", "CANCELLED", "REFUNDED"})
    @DisplayName("결제 상태가 COMPLETED가 아니면 취소할 수 없다")
    void shouldReturnFalse_whenPaymentStatusIsNotCompleted(PaymentStatus paymentStatus) {
        //given
        Payment payment = paymentOf(paymentStatus);

        //when
        boolean cancellable = payment.isCancellable();

        //then
        assertThat(cancellable).isFalse();
    }

    private Payment paymentOf(PaymentStatus paymentStatus) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .price(new Money(new BigDecimal("25.00")))
                .status(paymentStatus)
                .build();
    }

}
