package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.orderingsystem.restaurant.application.dto.request.CreateProductApplicationRequest;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.RestaurantProduct;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantProductRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateProductServiceTest {

    private ProductRepository productRepository;
    private RestaurantProductRepository restaurantProductRepository;
    private CreateProductService createProductService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        restaurantProductRepository = mock(RestaurantProductRepository.class);
        createProductService = new CreateProductService(productRepository, restaurantProductRepository);
    }

    @DisplayName("상품을 생성하면 상품 ID를 반환하고, 두 repository에 저장이 호출된다.")
    @Test
    void shouldCreateProductSuccessfully() {
        // given
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        CreateProductApplicationRequest request = CreateProductApplicationRequest.builder()
                .name("상품 이름")
                .price(BigDecimal.valueOf(10000))
                .available(true)
                .quantity(50)
                .build();

        Product mockProduct = Product.create(
                request.getName(), request.getAvailable(), request.getPrice(), request.getQuantity());
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

        // when
        UUID result = createProductService.create(request, restaurantId, ownerId);

        // then
        verify(productRepository, times(1)).save(any(Product.class));

        ArgumentCaptor<RestaurantProduct> captor = ArgumentCaptor.forClass(RestaurantProduct.class);
        verify(restaurantProductRepository, times(1)).save(captor.capture());

        RestaurantProduct savedMapping = captor.getValue();
        assertThat(savedMapping.getRestaurantId()).isEqualTo(restaurantId);
    }

}
