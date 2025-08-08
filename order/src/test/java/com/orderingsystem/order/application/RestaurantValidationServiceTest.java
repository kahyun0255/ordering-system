package com.orderingsystem.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.order.application.dto.RestaurantInfo;
import com.orderingsystem.order.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.order.domain.model.restaurant.Product;
import com.orderingsystem.order.domain.model.restaurant.Restaurant;
import com.orderingsystem.order.domain.model.restaurant.RestaurantProduct;
import com.orderingsystem.order.domain.repository.restaurant.ProductRepository;
import com.orderingsystem.order.domain.repository.restaurant.RestaurantProductRepository;
import com.orderingsystem.order.domain.repository.restaurant.RestaurantRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class RestaurantValidationServiceTest {

    @Autowired
    private RestaurantValidationService restaurantValidationService;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RestaurantProductRepository restaurantProductRepository;

    private final UUID restaurantId = UUID.randomUUID();
    private final UUID productId1 = UUID.randomUUID();
    private final UUID productId2 = UUID.randomUUID();

    @DisplayName("레스토랑과 판매 상품 조회를 할 수 있다.")
    @Test
    void findRestaurantWithProducts() {
        //given
        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .name("restaurantName")
                .active(true)
                .build());

        productRepository.save(Product.builder()
                .productId(productId1)
                .price(new Money(new BigDecimal("20.00")))
                .name("product1")
                .available(true)
                .build());

        productRepository.save(Product.builder()
                .productId(productId2)
                .price(new Money(new BigDecimal("25.00")))
                .name("productId2")
                .available(false)
                .build());

        restaurantProductRepository.save(RestaurantProduct.builder()
                .id(UUID.randomUUID())
                .productId(productId1)
                .restaurantId(restaurantId)
                .build());

        restaurantProductRepository.save(RestaurantProduct.builder()
                .id(UUID.randomUUID())
                .productId(productId2)
                .restaurantId(restaurantId)
                .build());

        //when
        RestaurantInfo restaurantInfo = restaurantValidationService.getRestaurantInfo(
                restaurantId, List.of(productId1, productId2));

        //then
        assertThat(restaurantInfo.getRestaurantId()).isEqualTo(restaurantId);
        assertThat(restaurantInfo.isActive()).isEqualTo(true);
        assertThat(restaurantInfo.getProducts()).hasSize(2)
                .extracting("productId", "available")
                .containsExactlyInAnyOrder(
                        tuple(productId1, true),
                        tuple(productId2, false));
    }

    @DisplayName("레스토랑이 존재하지 않으면 예외가 발생한다.")
    @Test
    void throwException_whenRestaurantDoesNotExist() {
        //given
        productRepository.save(Product.builder()
                .productId(productId1)
                .price(new Money(new BigDecimal("20.00")))
                .name("product1")
                .available(true)
                .build());

        productRepository.save(Product.builder()
                .productId(productId2)
                .price(new Money(new BigDecimal("25.00")))
                .name("productId2")
                .available(false)
                .build());

        restaurantProductRepository.save(RestaurantProduct.builder()
                .id(UUID.randomUUID())
                .productId(productId1)
                .restaurantId(restaurantId)
                .build());

        restaurantProductRepository.save(RestaurantProduct.builder()
                .id(UUID.randomUUID())
                .productId(productId2)
                .restaurantId(restaurantId)
                .build());

        //when, then
        assertThatThrownBy(
                () -> restaurantValidationService.getRestaurantInfo(restaurantId, List.of(productId1, productId2)))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessage("레스토랑 정보를 찾을 수 없습니다. Restaurant Id : " + restaurantId);
    }

    @DisplayName("물품이 존재하지 않으면 예외가 발생한다.")
    @Test
    void throwException_whenProductDoesNotExist() {
        //given
        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .name("restaurantName")
                .active(true)
                .build());

        productRepository.save(Product.builder()
                .productId(productId2)
                .price(new Money(new BigDecimal("25.00")))
                .name("productId2")
                .available(false)
                .build());

        restaurantProductRepository.save(RestaurantProduct.builder()
                .id(UUID.randomUUID())
                .productId(productId2)
                .restaurantId(restaurantId)
                .build());

        //when, then
        assertThatThrownBy(
                () -> restaurantValidationService.getRestaurantInfo(restaurantId, List.of(productId1, productId2)))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessage("요청한 상품 중 일부를 찾을 수 없습니다. RestaurantId : "+restaurantId+", productIds : "+List.of(productId1, productId2)+", findIds : "+List.of(productId2));
    }
}
