package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.restaurant.application.dto.request.ProductRequest;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.outbox.MessageType;
import com.orderingsystem.restaurant.domain.repository.OrderApprovalRepository;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class ProductStockFacadeCancelTest {

    @Autowired
    private ProductStockFacade productStockFacade;

    @MockitoBean
    private OrderCancelService orderCancelService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderApprovalRepository orderApprovalRepository;

    @Value("${key.stock}")
    private String stockKey;

    @Value("${key.reserved}")
    private String reserveKey;

    @Value("${key.confirmed}")
    private String confirmedKey;

    @Value("${key.history}")
    private String historyKey;

    private UUID productId;
    private UUID orderId;
    private UUID sagaId;
    private UUID restaurantId;

    private ProductRequest productRequest() {
        return ProductRequest.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .sagaId(sagaId)
                .restaurantId(restaurantId)
                .build();
    }

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        sagaId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();

        Product product = Product.builder()
                .productId(productId)
                .price(new Money(BigDecimal.valueOf(1000)))
                .name("상품1")
                .available(true)
                .quantity(100)
                .build();

        productRepository.save(product);

        redisTemplate.delete(stockKey + productId);
        redisTemplate.delete(reserveKey + productId);
        redisTemplate.delete(confirmedKey + orderId);
        redisTemplate.delete(historyKey + sagaId);
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete(stockKey + productId);
        redisTemplate.delete(reserveKey + productId);
        redisTemplate.delete(confirmedKey + orderId);
        redisTemplate.delete(historyKey + sagaId);
    }

    @DisplayName("주문 취소시 주문이 완료된 경우, Redis 및 DB 재고가 복구된다.")
    @Test
    void shouldRestoreStock_whenCancellingCompletedOrder() {
        //given
        OrderApproval orderApproval = OrderApproval.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .status(OrderApprovalStatus.ACCEPTED)
                .restaurantId(restaurantId)
                .build();
        orderApprovalRepository.save(orderApproval);

        redisTemplate.opsForValue().set(stockKey + productId, "100");

        productStockFacade.reserve(productId, 10, sagaId);
        productStockFacade.confirm(sagaId, orderId);

        String beforeStock = redisTemplate.opsForValue().get(stockKey + productId);
        assertThat(beforeStock).isEqualTo("90");

        Optional<Product> beforeProduct = productRepository.findById(productId);
        assertThat(beforeProduct.get().getQuantity()).isEqualTo(90L);

        ProductRequest productRequest = productRequest();
        given(orderCancelService.checkAndMarkProcessed(productRequest, MessageType.INVENTORY_COMPENSATE)).willReturn(
                true);

        //when
        productStockFacade.cancelByState(productRequest);

        //then
        String afterStock = redisTemplate.opsForValue().get(stockKey + productId);
        assertThat(afterStock).isEqualTo("100");

        Optional<Product> afterProduct = productRepository.findById(productId);
        assertThat(afterProduct.get().getQuantity()).isEqualTo(100L);

        String afterReserve = redisTemplate.opsForValue().get(reserveKey + productId);
        assertThat(afterReserve).isEqualTo("0");

        Optional<OrderApproval> afterOrderApproval = orderApprovalRepository.findById(orderApproval.getId());
        assertThat(afterOrderApproval.get().getStatus()).isEqualTo(OrderApprovalStatus.CANCELLED);
    }

    @DisplayName("주문 취소시 예약만 완료된 경우, Redis 에약이 취소된다.")
    @Test
    void shouldCancelRedisReservation_whenOrderIsOnlyReserved() {
        //given
        OrderApproval orderApproval = OrderApproval.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .status(OrderApprovalStatus.ACCEPTED)
                .restaurantId(restaurantId)
                .build();
        orderApprovalRepository.save(orderApproval);

        redisTemplate.opsForValue().set(stockKey + productId, "100");

        productStockFacade.reserve(productId, 10, sagaId);

        String beforeStock = redisTemplate.opsForValue().get(stockKey + productId);
        assertThat(beforeStock).isEqualTo("100");

        String beforeReserve = redisTemplate.opsForValue().get(reserveKey + productId);
        assertThat(beforeReserve).isEqualTo("10");

        Optional<Product> beforeProduct = productRepository.findById(productId);
        assertThat(beforeProduct.get().getQuantity()).isEqualTo(100L);

        ProductRequest productRequest = productRequest();
        given(orderCancelService.checkAndMarkProcessed(productRequest, MessageType.INVENTORY_COMPENSATE)).willReturn(
                true);

        //when
        productStockFacade.cancelByState(productRequest);

        //then
        String afterStock = redisTemplate.opsForValue().get(stockKey + productId);
        assertThat(afterStock).isEqualTo("100");

        String afterReserve = redisTemplate.opsForValue().get(reserveKey + productId);
        assertThat(afterReserve).isEqualTo("0");

        Optional<Product> afterProduct = productRepository.findById(productId);
        assertThat(afterProduct.get().getQuantity()).isEqualTo(100L);

        Optional<OrderApproval> afterOrderApproval = orderApprovalRepository.findById(orderApproval.getId());
        assertThat(afterOrderApproval.get().getStatus()).isEqualTo(OrderApprovalStatus.CANCELLED);
    }

    @DisplayName("주문이 완료되지 않았고, 예약도 진행되지 않았으면 아무 일도 일어나지 않는다.")
    @Test
    void shouldDoNothing_whenOrderNotConfirmedAndNotReserved() {
        //given
        long qtyBefore = productRepository.findById(productId).orElseThrow().getQuantity();
        Boolean hasConfirmedBefore = redisTemplate.hasKey(confirmedKey + orderId);
        Boolean hasHistoryBefore = redisTemplate.hasKey(historyKey + sagaId);
        Boolean hasReserveBefore = redisTemplate.hasKey(reserveKey + productId);

        assertThat(hasConfirmedBefore).isFalse();
        assertThat(hasHistoryBefore).isFalse();
        assertThat(hasReserveBefore).isFalse();

        //when
        productStockFacade.cancelByState(productRequest());

        //then
        assertThat(redisTemplate.hasKey(confirmedKey + orderId)).isFalse();
        assertThat(redisTemplate.hasKey(historyKey + sagaId)).isFalse();
        assertThat(redisTemplate.hasKey(reserveKey + productId)).isFalse();

        long qtyAfter = productRepository.findById(productId).orElseThrow().getQuantity();
        assertThat(qtyAfter).isEqualTo(qtyBefore);

        assertThat(orderApprovalRepository.findByOrderId(orderId)).isEmpty();
    }

    @DisplayName("메시지가 이미 처리되었을 경우, 아무런 일도 발생하지 않고 중단된다.")
    @Test
    void shouldDoNothing_whenMessageAlreadyProcessed() {
        //given
        OrderApproval orderApproval = OrderApproval.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .status(OrderApprovalStatus.ACCEPTED)
                .restaurantId(restaurantId)
                .build();
        orderApprovalRepository.save(orderApproval);

        redisTemplate.opsForValue().set(stockKey + productId, "100");

        productStockFacade.reserve(productId, 10, sagaId);
        productStockFacade.confirm(sagaId, orderId);

        String beforeStock = redisTemplate.opsForValue().get(stockKey + productId);
        assertThat(beforeStock).isEqualTo("90");

        Optional<Product> beforeProduct = productRepository.findById(productId);
        assertThat(beforeProduct.get().getQuantity()).isEqualTo(90L);

        ProductRequest productRequest = productRequest();
        given(orderCancelService.checkAndMarkProcessed(productRequest, MessageType.INVENTORY_COMPENSATE)).willReturn(
                false);

        //when
        productStockFacade.cancelByState(productRequest);

        //then
        String afterStock = redisTemplate.opsForValue().get(stockKey + productId);
        assertThat(afterStock).isEqualTo("90");

        Optional<Product> afterProduct = productRepository.findById(productId);
        assertThat(afterProduct.get().getQuantity()).isEqualTo(90L);

        Optional<OrderApproval> afterOrderApproval = orderApprovalRepository.findById(orderApproval.getId());
        assertThat(afterOrderApproval.get().getStatus()).isEqualTo(orderApproval.getStatus());
    }

}
