package com.orderingsystem.restaurant.domain.model.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderOutboxTest {

    @DisplayName("id가 같으면 OrderOutboxId는 같은 객체이다.")
    @Test
    void orderEqualsAndHashCodeWithEquals() {
        //given
        UUID orderOutboxId = UUID.randomUUID();
        OrderOutbox orderOutbox1 = OrderOutbox.builder()
                .id(orderOutboxId)
                .type("type")
                .payload("payload")
                .orderApprovalStatus(OrderApprovalStatus.APPROVED)
                .sagaId(UUID.randomUUID())
                .build();

        OrderOutbox orderOutbox2 = OrderOutbox.builder()
                .id(orderOutboxId)
                .type("type1")
                .payload("payload1")
                .orderApprovalStatus(OrderApprovalStatus.REJECTED)
                .sagaId(UUID.randomUUID())
                .build();

        //when, then
        assertThat(orderOutbox1.equals(orderOutbox2)).isTrue();
        assertThat(orderOutbox1.hashCode()).isEqualTo(orderOutbox2.hashCode());
    }

    @DisplayName("id가 다르면 OrderOutboxId는 다른 객체이다.")
    @Test
    void orderEqualsAndHashCodeWithDifference() {
        //given
        UUID sagaId = UUID.randomUUID();
        OrderOutbox orderOutbox1 = OrderOutbox.builder()
                .id(UUID.randomUUID())
                .type("type")
                .payload("payload")
                .orderApprovalStatus(OrderApprovalStatus.APPROVED)
                .sagaId(sagaId)
                .build();

        OrderOutbox orderOutbox2 = OrderOutbox.builder()
                .id(UUID.randomUUID())
                .type("type")
                .payload("payload")
                .orderApprovalStatus(OrderApprovalStatus.REJECTED)
                .sagaId(sagaId)
                .build();

        //when, then
        assertThat(orderOutbox1.equals(orderOutbox2)).isFalse();
        assertThat(orderOutbox1.equals(null)).isFalse();
        assertThat(orderOutbox1.hashCode()).isNotEqualTo(orderOutbox2.hashCode());
    }
}