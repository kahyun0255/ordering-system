package com.orderingsystem.restaurant.application.listener;

import com.orderingsystem.restaurant.application.InventoryFacade;
import com.orderingsystem.restaurant.domain.event.order.orderaccept.OrderAcceptedEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAcceptEventListener {

    private final InventoryFacade inventoryFacade;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderAccepted(OrderAcceptedEvent orderAcceptedEvent){
        UUID sagaId = orderAcceptedEvent.getSagaId();
        UUID orderId= orderAcceptedEvent.getOrderApproval().getOrderId();
        inventoryFacade.confirm(sagaId, orderId);
        log.info("{} 주문 접수 이벤트 수신 후 재고 확정 처리 완료.", sagaId);
    }

}
