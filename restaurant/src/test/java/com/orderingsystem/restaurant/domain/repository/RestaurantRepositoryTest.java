package com.orderingsystem.restaurant.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantInfoView;
import com.orderingsystem.restaurant.domain.model.RestaurantProduct;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
class RestaurantRepositoryTest {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RestaurantProductRepository restaurantProductRepository;

    private final UUID restaurantId = UUID.randomUUID();
    private final UUID productId1 = UUID.randomUUID();
    private final UUID productId2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .status(RestaurantStatus.ACTIVE)
                .name("restaurant")
                .build());

        productRepository.save(Product.builder()
                .productId(productId1)
                .name("product1")
                .available(true)
                .price(new Money(new BigDecimal("25.00")))
                .build());

        productRepository.save(Product.builder()
                .productId(productId2)
                .name("product2")
                .available(true)
                .price(new Money(new BigDecimal("30.00")))
                .build());

        restaurantProductRepository.save(RestaurantProduct.builder()
                        .id(UUID.randomUUID())
                        .restaurantId(restaurantId)
                        .productId(productId1)
                .build());

        restaurantProductRepository.save(RestaurantProduct.builder()
                .id(UUID.randomUUID())
                .restaurantId(restaurantId)
                .productId(productId2)
                .build());
    }

    @DisplayName("레스토랑 물품 정보 조회에 성공한다.")
    @Test
    void findRestaurantInfo() {
        //when
        List<RestaurantInfoView> restaurantInfo = restaurantRepository.findRestaurantInfo(restaurantId,
                List.of(productId1, productId2));

        //then
        assertThat(restaurantInfo).hasSize(2)
                .extracting("productId", "productName", "restaurantId")
                .containsExactlyInAnyOrder(
                        tuple(productId1, "product1", restaurantId),
                        tuple(productId2, "product2", restaurantId)
                );

    }
}
