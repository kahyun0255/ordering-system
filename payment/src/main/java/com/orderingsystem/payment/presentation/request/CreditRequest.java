package com.orderingsystem.payment.presentation.request;

import com.orderingsystem.payment.application.dto.request.CreditApplicationRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreditRequest {

    @NotNull(message = "amount는 필수입니다.")
    @Positive(message = "amount는 0보다 커야합니다.")
    private BigDecimal amount;

    public CreditApplicationRequest toApplicationRequest(){
        return CreditApplicationRequest.builder()
                .amount(this.amount)
                .build();
    }

}
