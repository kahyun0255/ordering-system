package com.orderingsystem.restaurant.domain.repository.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.domain.model.outbox.OrderOutbox;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
class OrderOutboxRepositoryTest {

    @Autowired
    private OrderOutboxRepository orderOutboxRepository;

    private final UUID sagaId1 = UUID.randomUUID();
    private final UUID sagaId2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        OrderOutbox orderOutbox1 = OrderOutbox.builder()
                .type("type1")
                .id(UUID.randomUUID())
                .sagaId(sagaId1)
                .outboxStatus(OutboxStatus.STARTED)
                .orderApprovalStatus(OrderApprovalStatus.APPROVED)
                .payload("payload")
                .build();

        OrderOutbox orderOutbox2 = OrderOutbox.builder()
                .type("type1")
                .id(UUID.randomUUID())
                .sagaId(sagaId1)
                .outboxStatus(OutboxStatus.STARTED)
                .orderApprovalStatus(OrderApprovalStatus.APPROVED)
                .payload("payload")
                .build();

        OrderOutbox orderOutbox3 = OrderOutbox.builder()
                .type("type2")
                .id(UUID.randomUUID())
                .sagaId(sagaId1)
                .outboxStatus(OutboxStatus.STARTED)
                .orderApprovalStatus(OrderApprovalStatus.APPROVED)
                .payload("payload")
                .build();

        OrderOutbox orderOutbox4 = OrderOutbox.builder()
                .type("type1")
                .id(UUID.randomUUID())
                .sagaId(sagaId1)
                .outboxStatus(OutboxStatus.COMPLETED)
                .orderApprovalStatus(OrderApprovalStatus.APPROVED)
                .payload("payload")
                .build();

        OrderOutbox orderOutbox5 = OrderOutbox.builder()
                .type("type1")
                .id(UUID.randomUUID())
                .sagaId(sagaId2)
                .outboxStatus(OutboxStatus.COMPLETED)
                .orderApprovalStatus(OrderApprovalStatus.APPROVED)
                .payload("payload")
                .build();

        OrderOutbox orderOutbox6 = OrderOutbox.builder()
                .type("type1")
                .id(UUID.randomUUID())
                .sagaId(sagaId2)
                .outboxStatus(OutboxStatus.STARTED)
                .orderApprovalStatus(OrderApprovalStatus.APPROVED)
                .payload("payload")
                .build();

        orderOutboxRepository.saveAll(
                List.of(orderOutbox1, orderOutbox2, orderOutbox3, orderOutbox4, orderOutbox5, orderOutbox6));
    }

    @DisplayName("Type, SagaID, OutboxStatus를 받아 Order Outbox 메시지를 조회한다.")
    @Test
    void findByTypeAndSagaIdAndOutboxStatus() {
        //when
        List<OrderOutbox> orderOutboxes = orderOutboxRepository.findByTypeAndSagaIdAndOutboxStatus(
                "type1", sagaId1, OutboxStatus.STARTED);

        //then
        assertThat(orderOutboxes).hasSize(2);
    }

    @DisplayName("Type, OutboxStatus를 받아 Order Outbox 메시지를 조회한다.")
    @Test
    void findByTypeAndOutboxStatus() {
        //when
        Optional<List<OrderOutbox>> orderOutboxes = orderOutboxRepository.findByTypeAndOutboxStatus(
                "type1", OutboxStatus.STARTED);

        //then
        assertThat(orderOutboxes).isPresent();
        assertThat(orderOutboxes.get()).hasSize(3);
    }

    @DisplayName("Type, OutboxStatus를 받아 Order Outbox 메시지 삭제에 성공한다.")
    @Test
    void deleteAllByTypeAndOutboxStatus() {
        //given
        assertThat(orderOutboxRepository.count()).isEqualTo(6L);

        //when
        orderOutboxRepository.deleteAllByTypeAndOutboxStatus("type1", OutboxStatus.COMPLETED);

        //then
        assertThat(orderOutboxRepository.count()).isEqualTo(4L);
    }

    @DisplayName("Type, SagaID, OrderApprovalStatus, OutboxStatus를 받아 Order Outbox 메시지가 있으면 True를 반환한다.")
    @Test
    void existsByTypeAndSagaIdAndOrderApprovalStatusAndOutboxStatus_True() {
        //when
        boolean exists = orderOutboxRepository.existsByTypeAndSagaIdAndOrderApprovalStatusAndOutboxStatus(
                "type1", sagaId1, OrderApprovalStatus.APPROVED, OutboxStatus.STARTED);

        //then
        assertThat(exists).isTrue();
    }

    @DisplayName("Type, SagaID, OrderApprovalStatus, OutboxStatus를 받아 Order Outbox 메시지가 없으면 False를 반환한다.")
    @Test
    void existsByTypeAndSagaIdAndOrderApprovalStatusAndOutboxStatus_False() {
        //when
        boolean exists = orderOutboxRepository.existsByTypeAndSagaIdAndOrderApprovalStatusAndOutboxStatus(
                "type2", sagaId1, OrderApprovalStatus.APPROVED, OutboxStatus.COMPLETED);

        //then
        assertThat(exists).isFalse();
    }

}
