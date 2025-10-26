package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.restaurant.application.dto.request.UpdateProductApplicationRequest;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class ProductFacadeUpdateIntegrationTest extends ApplicationTestSupport {

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID nonOwnerId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();
    private final Product product = Product.builder()
            .productId(UUID.randomUUID())
            .price(new Money(BigDecimal.valueOf(10000)))
            .available(true)
            .name("상품")
            .quantity(10)
            .build();

    @Value("${key.stock}")
    private String stockKey;

    @BeforeEach
    void setUp() {
        Owner owner = Owner.builder()
                .name("owner")
                .userId(ownerId)
                .build();
        ownerRepository.save(owner);

        Owner nonOwner = Owner.builder()
                .name("Not the Owner")
                .userId(nonOwnerId)
                .build();
        ownerRepository.save(nonOwner);

        RestaurantOwnership restaurantOwnership = RestaurantOwnership.builder()
                .ownerId(ownerId)
                .restaurantId(restaurantId)
                .build();
        restaurantOwnershipRepository.save(restaurantOwnership);

        productRepository.save(product);

        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set(stockKey + product.getProductId(), "10");
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAllInBatch();

    }

    @DisplayName("레스토랑의 소유자일경우 관리자 승인 대기, 영업 전, 영업 중, 일시 휴업 상태의 레스토랑 상품 정보 변경시 Redis 재고 값도 변경된다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("provideUpdatableRestaurantStatuses")
    void shouldUpdateProduct_whenOwnerAndRestaurantStatusIsAllowed(String status, RestaurantStatus restaurantStatus)
            throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(restaurantStatus)
                .build();
        restaurantRepository.save(restaurant);

        UpdateProductApplicationRequest request = UpdateProductApplicationRequest.builder()
                .name("변경할 상품 이름")
                .price(BigDecimal.valueOf(50.00))
                .available(false)
                .quantity(100000000)
                .build();

        //when
        productFacade.update(ownerId, restaurantId, product.getProductId(), request);

        //then
        Optional<Product> after = productRepository.findById(product.getProductId());
        assertThat(after).isPresent();
        assertThat(after.get().getQuantity()).isEqualTo(request.getQuantity());
        assertThat(after.get().getName()).isEqualTo(request.getName());

        String redisStock = redisTemplate.opsForValue().get(stockKey + product.getProductId());
        assertThat(redisStock).isEqualTo(String.valueOf(request.getQuantity()));
    }

    private static Stream<Arguments> provideUpdatableRestaurantStatuses() {
        return Stream.of(
                Arguments.of("관리자 승인 대기", RestaurantStatus.PENDING_APPROVAL),
                Arguments.of("영업 전", RestaurantStatus.PRE_OPEN),
                Arguments.of("영업 중", RestaurantStatus.ACTIVE),
                Arguments.of("일시 휴업", RestaurantStatus.TEMP_CLOSED)
        );
    }

    @DisplayName("레스토랑의 소유자라도 폐업, 영업 정지 상태의 레스토랑에는 상품 정보 업데이트와 Redis 재고 업데이트가 불가능하고 예외를 반환한다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("provideNonUpdatableRestaurantStatuses")
    void shouldDenyProductUpdate_whenRestaurantStatusIsClosedOrSuspended(String status,
                                                                        RestaurantStatus restaurantStatus) {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(restaurantStatus)
                .build();
        restaurantRepository.save(restaurant);

        UpdateProductApplicationRequest request = UpdateProductApplicationRequest.builder()
                .name("변경할 상품 이름")
                .price(BigDecimal.valueOf(50.00))
                .available(false)
                .quantity(100000000)
                .build();

        //when, then
        assertThatThrownBy(() -> productFacade.update(ownerId, restaurantId, product.getProductId(), request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("현재 상태의 레스토랑에서는 상품을 관리할 수 없습니다.");

        Optional<Product> after = productRepository.findById(product.getProductId());
        assertThat(after.get().getQuantity()).isEqualTo(product.getQuantity());
        assertThat(after.get().getQuantity()).isNotEqualTo(request.getQuantity());

        String redisStock = redisTemplate.opsForValue().get(stockKey + product.getProductId());
        assertThat(redisStock).isNotEqualTo(String.valueOf(request.getQuantity()));
        assertThat(redisStock).isEqualTo(String.valueOf(product.getQuantity()));
    }

    private static Stream<Arguments> provideNonUpdatableRestaurantStatuses() {
        return Stream.of(
                Arguments.of("폐업", RestaurantStatus.PERM_CLOSED),
                Arguments.of("영업 정지", RestaurantStatus.SUSPENDED)
        );
    }

}
