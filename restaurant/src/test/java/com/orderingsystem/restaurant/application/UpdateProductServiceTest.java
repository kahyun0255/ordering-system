package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.restaurant.application.dto.request.UpdateProductApplicationRequest;
import com.orderingsystem.restaurant.domain.exception.ProductNotFoundException;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private UpdateProductService updateProductService;

    @DisplayName("필드가 변경되면 각각의 업데이트 메서드가 호출된다.")
    @Test
    void shouldUpdateAllFields_whenValuesAreDifferent() {
        //given
        UUID productId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Product product = mock(Product.class);

        given(product.getName()).willReturn("기존 이름");
        given(product.getPrice()).willReturn(new Money(BigDecimal.valueOf(100.00)));
        given(product.isAvailable()).willReturn(true);
        given(product.getQuantity()).willReturn(10);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        UpdateProductApplicationRequest request = UpdateProductApplicationRequest.builder()
                .name("변경할 상품 이름")
                .price(BigDecimal.valueOf(50.00))
                .available(false)
                .quantity(100)
                .build();

        //when
        updateProductService.updateProduct(ownerId, restaurantId, productId, request);

        //then
        verify(product).updateName("변경할 상품 이름");
        verify(product).updatePrice(BigDecimal.valueOf(50.00));
        verify(product).updateAvailability(false);
        verify(product).updateQuantity(100);
    }

    @DisplayName("상품이 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenProductDoesNotExist() {
        //given
        UUID productId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        given(productRepository.findById(productId)).willReturn(Optional.empty());

        UpdateProductApplicationRequest request = UpdateProductApplicationRequest.builder()
                .name("변경할 상품 이름")
                .price(BigDecimal.valueOf(50.00))
                .available(false)
                .quantity(100)
                .build();

        //when, then
        assertThatThrownBy(() -> updateProductService.updateProduct(ownerId, restaurantId, productId, request))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("상품 정보를 찾을 수 없습니다.");
    }

    @DisplayName("요청 값이 기존 값과 같다면 업데이트 메서드를 호출하지 않는다.")
    @Test
    void shouldNotUpdateFields_whenValuesAreSame() {
        //given
        UUID productId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Product product = mock(Product.class);

        given(product.getName()).willReturn("기존 이름");
        given(product.getPrice()).willReturn(new Money(BigDecimal.valueOf(100.00)));
        given(product.isAvailable()).willReturn(true);
        given(product.getQuantity()).willReturn(10);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        UpdateProductApplicationRequest request = UpdateProductApplicationRequest.builder()
                .name("기존 이름")
                .price(BigDecimal.valueOf(100.00))
                .available(true)
                .quantity(10)
                .build();

        //when
        updateProductService.updateProduct(ownerId, restaurantId, productId, request);

        //then
        verify(product, never()).updateName(any());
        verify(product, never()).updatePrice(any());
        verify(product, never()).updateAvailability(any());
        verify(product, never()).updateQuantity(any());
    }

    @DisplayName("요청 값이 null이면 업데이트 메서드를 호출하지 않는다.")
    @Test
    void shouldNotCallUpdateMethods_whenRequestValuesAreNull() {
        //given
        UUID productId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Product product = mock(Product.class);

        given(product.getName()).willReturn("기존 이름");
        given(product.getPrice()).willReturn(new Money(BigDecimal.valueOf(100.00)));
        given(product.isAvailable()).willReturn(true);
        given(product.getQuantity()).willReturn(10);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        UpdateProductApplicationRequest request = UpdateProductApplicationRequest.builder()
                .available(false)
                .quantity(100)
                .build();

        //when
        updateProductService.updateProduct(ownerId, restaurantId, productId, request);

        //then
        verify(product, never()).updateName(any());
        verify(product, never()).updatePrice(any());

        verify(product).updateAvailability(false);
        verify(product).updateQuantity(100);
    }

}
