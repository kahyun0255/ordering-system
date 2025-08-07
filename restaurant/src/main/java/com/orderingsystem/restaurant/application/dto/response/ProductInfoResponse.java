package com.orderingsystem.restaurant.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductInfoResponse {

    private UUID productId;
    private String name;
    private BigDecimal price;
    private boolean available;

}
