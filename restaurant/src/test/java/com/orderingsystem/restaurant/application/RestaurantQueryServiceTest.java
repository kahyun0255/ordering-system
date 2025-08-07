package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.restaurant.application.dto.response.RestaurantInfoResponse;
import com.orderingsystem.restaurant.application.exception.RestaurantApplicationException;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantProduct;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantProductRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
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
class RestaurantQueryServiceTest {

    @Autowired
    private RestaurantQueryService restaurantQueryService;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RestaurantProductRepository restaurantProductRepository;

    private final UUID restaurantId = UUID.randomUUID();
    private final UUID productId1 = UUID.randomUUID();
    private final UUID productId2 = UUID.randomUUID();
    private final UUID productId3 = UUID.randomUUID();
    private final BigDecimal productPrice = new BigDecimal("25.00");

    @DisplayName("레스토랑이 판매하는 제품 정보들을 조회할 수 있다.")
    @Test
    void findProductsSoldByRestaurant() {
        //given
        saveRestaurant();
        saveProduct(productId1, true);
        saveProduct(productId2, true);
        saveProduct(productId3, false);
        saveRestaurantProduct(restaurantId, productId1);
        saveRestaurantProduct(restaurantId, productId2);
        saveRestaurantProduct(restaurantId, productId3);

        //when
        RestaurantInfoResponse restaurantInfo = restaurantQueryService.getRestaurantInfo(restaurantId,
                List.of(productId1, productId2, productId3));

        //then
        assertThat(restaurantInfo.getRestaurantId()).isEqualTo(restaurantId);
        assertThat(restaurantInfo.isActive()).isTrue();
        assertThat(restaurantInfo.getProducts()).hasSize(3)
                .extracting("productId", "available")
                .containsExactlyInAnyOrder(
                        tuple(productId1, true),
                        tuple(productId2, true),
                        tuple(productId3, false));
    }

    @DisplayName("레스토랑 정보를 찾을 수 없으면 예외가 발생한다.")
    @Test
    void notFoundRestaurant() {
        //given
        saveProduct(productId1, true);
        saveProduct(productId2, true);
        saveProduct(productId3, false);
        saveRestaurantProduct(restaurantId, productId1);
        saveRestaurantProduct(restaurantId, productId2);
        saveRestaurantProduct(restaurantId, productId3);

        //when, then
        assertThatThrownBy(()->restaurantQueryService.getRestaurantInfo(restaurantId, List.of(productId1, productId2, productId3)))
                .isInstanceOf(RestaurantApplicationException.class)
                .hasMessage("레스토랑 정보를 찾을 수 없습니다.");
    }

    private void saveRestaurant(Boolean active) {
        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .name("restaurant")
                .active(active)
                .build());
    }

    private void saveRestaurant() {
        saveRestaurant(true);
    }

    private void saveProduct(UUID productId, boolean available) {
        productRepository.save(Product.builder()
                .productId(productId)
                .name("product")
                .price(new Money(productPrice))
                .available(available)
                .build());
    }

    private void saveRestaurantProduct(UUID restaurantId, UUID productId) {
        restaurantProductRepository.save(RestaurantProduct.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .restaurantId(restaurantId)
                .build());
    }

}
