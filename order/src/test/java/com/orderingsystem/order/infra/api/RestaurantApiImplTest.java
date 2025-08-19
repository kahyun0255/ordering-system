package com.orderingsystem.order.infra.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.Money;
import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import com.orderingsystem.order.application.dto.request.OrderAddressApplicationRequest;
import com.orderingsystem.order.application.dto.request.OrderItemApplicationRequest;
import com.orderingsystem.order.domain.exception.RestaurantServiceException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RestaurantApiImplTest {

    private static MockWebServer server;
    private RestaurantApiImpl restaurantApi;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void beforeAll() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterAll
    static void afterAll() throws IOException {
        server.shutdown();
    }

    @BeforeEach
    void setUp() {
        String baseUrl = server.url("/").toString();
        restaurantApi = new RestaurantApiImpl(baseUrl);
    }

    @DisplayName("200 OK를 반환받으면 예외가 발생하지 않고 통과한다.")
    @Test
    void success_200_ok() {
        //given
        server.enqueue(new MockResponse().setResponseCode(200));

        UUID sagaId = UUID.randomUUID();
        CreateOrderApplicationRequest request = getCreateOrderApplicationRequest();

        //when, then
        assertThatCode(()->restaurantApi.validRestaurantAndProducts(request, sagaId))
                .doesNotThrowAnyException();

        RecordedRequest recordedRequest;
        try{
            recordedRequest = server.takeRequest();
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getPath()).isEqualTo("/internal/restaurants/"+request.getRestaurantId()+"/validate");

            JsonNode body = objectMapper.readTree(recordedRequest.getBody().readUtf8());
            assertThat(body.get("sagaId").asText()).isEqualTo(sagaId.toString());
            assertThat(body.get("totalPrice").decimalValue()).isEqualByComparingTo(request.getPrice());
            assertThat(body.get("items")).hasSize(2);
        } catch (InterruptedException | IOException e) {
            Assertions.fail("요청 바디 검증 실패", e);
        }
    }

    @DisplayName("4xx 오류 반환 테스트")
    @Test
    void client_error_4xx_passthrough() {
        //given
        String errorJson = "{\"code\":\"Bad Request\",\"message\":\"상품 가격이 일치하지 않습니다.\"}";
        server.enqueue(new MockResponse().setResponseCode(400).setBody(errorJson));

        CreateOrderApplicationRequest request = getCreateOrderApplicationRequest();

        //when, then
        assertThatThrownBy(()->restaurantApi.validRestaurantAndProducts(request, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(400);
                    assertThat(rse.getReason()).isEqualTo(errorJson);
                });
    }

    @DisplayName("5xx 오류 반환 테스트")
    @Test
    void server_error_5xx_maps_to_ServiceException() {
        //given
        server.enqueue(new MockResponse().setResponseCode(500).setBody("DB down"));

        CreateOrderApplicationRequest request = getCreateOrderApplicationRequest();

        //when, then
        assertThatThrownBy(() -> restaurantApi.validRestaurantAndProducts(request, UUID.randomUUID()))
                .isInstanceOf(RestaurantServiceException.class)
                .hasMessage("DB down");
    }

    @DisplayName("네트워크 오류 반환 테스트")
    @Test
    void network_error_maps_to_503() throws IOException {
        //given
        server.shutdown();

        RestaurantApiImpl apiWithDeadUrl = new RestaurantApiImpl("http://localhost:65001");
        CreateOrderApplicationRequest request = getCreateOrderApplicationRequest();

        //when, then
        assertThatThrownBy(() -> apiWithDeadUrl.validRestaurantAndProducts(request, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(503);
                    assertThat(rse.getReason()).isEqualTo("레스토랑 서비스 응답 지연/네트워크 오류");
                });

        // 다음 테스트를 위해 다시 서버 기동
        server = new MockWebServer();
        server.start();
        restaurantApi = new RestaurantApiImpl(server.url("/").toString());
    }

    private CreateOrderApplicationRequest getCreateOrderApplicationRequest() {
        Money product1Price = new Money(new BigDecimal("25.00"));
        Money product2Price = new Money(new BigDecimal("20.00"));

        return CreateOrderApplicationRequest.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(OrderAddressApplicationRequest.builder()
                        .street("street1")
                        .postalCode("123-78")
                        .city("city1")
                        .build())
                .price(product1Price.add(product2Price.multiply(2)).getAmount())
                .items(List.of(OrderItemApplicationRequest.builder()
                                .productId(UUID.randomUUID())
                                .quantity(1)
                                .price(product1Price.getAmount())
                                .subTotal(product1Price.getAmount())
                                .build(),
                        OrderItemApplicationRequest.builder()
                                .productId(UUID.randomUUID())
                                .quantity(2)
                                .price(product2Price.getAmount())
                                .subTotal(product2Price.multiply(2).getAmount())
                                .build()))
                .build();
    }
}
