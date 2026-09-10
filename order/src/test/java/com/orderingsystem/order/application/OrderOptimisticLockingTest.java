package com.orderingsystem.order.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class OrderOptimisticLockingTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    @DisplayName("동시에 두 트랜잭션이 같은 버전의 Order를 수정하고 커밋하려 하면 두 번째 커밋은 예외가 발생한다")
    void shouldThrowOptimisticLockingFailure_whenConcurrentUpdateOnSameVersion() {
        //given
        Order saved = orderRepository.saveAndFlush(newPendingOrder());
        entityManager.clear();

        Order paymentThreadView = orderRepository.findById(saved.getId()).orElseThrow();
        entityManager.clear();

        Order couponThreadView = orderRepository.findById(saved.getId()).orElseThrow();

        //when
        couponThreadView.couponCompleted();
        orderRepository.saveAndFlush(couponThreadView);
        entityManager.clear();

        //then
        paymentThreadView.pay();
        assertThatThrownBy(() -> orderRepository.saveAndFlush(paymentThreadView))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("충돌 후 다시 조회해서 재시도하면 최신 상태(상대방이 커밋한 값)를 반영해 정상 저장된다")
    void shouldSucceedAndReflectLatestState_whenRetriedWithFreshReadAfterConflict() {
        //given
        Order saved = orderRepository.saveAndFlush(newPendingOrder());
        entityManager.clear();

        Order couponThreadView = orderRepository.findById(saved.getId()).orElseThrow();
        couponThreadView.couponCompleted();
        orderRepository.saveAndFlush(couponThreadView);
        entityManager.clear();

        //when
        Order refetched = orderRepository.findById(saved.getId()).orElseThrow();
        refetched.pay();
        orderRepository.saveAndFlush(refetched);

        //then
        entityManager.clear();
        Order result = orderRepository.findById(saved.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(result.isPaymentCompleted()).isTrue();
        org.assertj.core.api.Assertions.assertThat(result.isCouponCompleted()).isTrue();
    }

    private Order newPendingOrder() {
        Order order = Order.builder()
                .price(new Money(BigDecimal.valueOf(10_000)))
                .items(List.of())
                .couponIds(List.of(1L))
                .build();
        order.initializeOrder();
        return order;
    }

}
