package com.orderingsystem.restaurant.application.listener;

import com.orderingsystem.restaurant.application.RestaurantStockFacade;
import com.orderingsystem.restaurant.domain.event.orderapproval.OrderApprovedEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderApprovedEventListener {

    private final RestaurantStockFacade restaurantStockFacade;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderApproved(OrderApprovedEvent orderApprovedEvent){
        UUID sagaId = orderApprovedEvent.getSagaId();
        restaurantStockFacade.confirm(sagaId);
        log.info("{} 주문 승인 이벤트 수신 후 재고 확정 처리 완료.", sagaId);
    }

}
