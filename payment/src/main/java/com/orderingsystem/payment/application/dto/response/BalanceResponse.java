package com.orderingsystem.payment.application.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BalanceResponse {

    private BigDecimal balance;

}
