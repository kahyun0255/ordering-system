package com.orderingsystem.restaurant.domain.model;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OrderDetail {

    private final UUID orderId;
    private OrderStatus orderStatus;
    private Money totalAmount;
    private final List<Product> products;

}
