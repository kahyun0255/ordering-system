package com.orderingsystem.restaurant.presentation.request;

import com.orderingsystem.restaurant.application.dto.request.CreateProductApplicationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Valid
public class CreateProductRequest {

    @NotBlank(message = "상품 이름을 입력해주세요. (2자 이상 30자 이하)")
    @Size(min = 2, max = 30, message = "상품 이름은 2자 이상 30자 이하로 입력해주세요.")
    private String name;

    @NotNull(message = "상품 가격을 입력해주세요.")
    @Positive(message = "상품 가격은 0보다 커야 합니다.")
    private BigDecimal price;

    @NotNull(message = "상품 판매 여부를 선택해주세요.")
    private Boolean available;

    @NotNull(message = "상품 재고를 입력해주세요.")
    @PositiveOrZero(message = "상품 재고는 0 이상이어야 합니다.")
    private Integer quantity;

    public @Valid CreateProductApplicationRequest toApplicationRequest(){
        return CreateProductApplicationRequest.builder()
                .name(this.name)
                .price(this.price)
                .available(this.available)
                .quantity(this.quantity)
                .build();
    }

}
