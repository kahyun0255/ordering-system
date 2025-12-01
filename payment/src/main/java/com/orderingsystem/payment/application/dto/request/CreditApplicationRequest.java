package com.orderingsystem.payment.application.dto.request;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreditApplicationRequest {

    private BigDecimal amount;

}
