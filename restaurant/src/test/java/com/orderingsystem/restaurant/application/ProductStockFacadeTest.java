package com.orderingsystem.restaurant.application;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductStockFacadeTest {

    @Mock
    private StockCachePort stockCachePort;

    @InjectMocks
    private ProductStockFacade productStockFacade;

    @DisplayName("상품 재고 예약이 정상적으로 위임된다.")
    @Test
    void testReserveDelegation() {
        //given
        UUID productId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        int quantity = 3;

        //when
        productStockFacade.reserve(productId, quantity, sagaId);

        //then
        verify(stockCachePort, times(1))
                .reserve(productId, quantity, sagaId);
    }

}