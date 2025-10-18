package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.orderingsystem.restaurant.domain.exception.ProductNotFoundException;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private DeleteProductService deleteProductService;

    @DisplayName("상품이 존재하면 상품 삭제에 성공한다.")
    @Test
    void shouldDeleteProductSuccessfully_whenProductExists() {
        //given
        UUID productId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Product product = mock(Product.class);
        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        //when
        deleteProductService.deleteProduct(restaurantId, productId, ownerId);

        //then
        verify(productRepository).findById(productId);
        verify(product).delete();
    }

    @DisplayName("상품이 존재하지 않으면 ProductNotFoundException이 발생한다.")
    @Test
    void shouldThrowException_whenProductDoesNotExist() {
        // given
        UUID productId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> deleteProductService.deleteProduct(restaurantId, productId, ownerId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("상품 정보를 찾을 수 없습니다.");
    }

}

