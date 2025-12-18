package com.orderingsystem.order.infra.api;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import com.orderingsystem.order.application.port.out.RestaurantApi;
import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import com.orderingsystem.order.domain.exception.RestaurantServiceException;
import com.orderingsystem.order.infra.api.dto.request.RestaurantValidationRequest;
import io.netty.handler.timeout.ReadTimeoutException;
import java.time.Duration;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class RestaurantApiImpl implements RestaurantApi {

    private final WebClient webClient;

    public RestaurantApiImpl(@Value("${url.restaurant}") String serviceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(serviceUrl)
                .build();
    }

    @Override
    public void validRestaurantAndProducts(CreateOrderApplicationRequest createOrderRequest, UUID sagaId) {
        try {
            webClient.post()
                    .uri("/internal/restaurants/{restaurantId}/validate", createOrderRequest.getRestaurantId())
                    .bodyValue(toValidationCommand(createOrderRequest, sagaId))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, resp ->
                            resp.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        return Mono.error(new ResponseStatusException(resp.statusCode(), body));
                                    })
                    )
                    .onStatus(HttpStatusCode::is5xxServerError, resp -> resp.bodyToMono(String.class)
                            .defaultIfEmpty("레스토랑 서비스 오류")
                            .flatMap(body -> Mono.error(new RestaurantServiceException(body)))
                    )
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(3));
        } catch (ReadTimeoutException | WebClientRequestException e) {
            log.error("레스토랑 서비스 응답 지연/네트워크 오류, Saga Id : {}, error : {} ", sagaId, e.toString());
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "레스토랑 서비스 응답 지연/네트워크 오류");
        }
    }

    private RestaurantValidationRequest toValidationCommand(CreateOrderApplicationRequest createOrderRequest,
                                                            UUID sagaId) {
        return RestaurantValidationRequest.builder()
                .sagaId(sagaId)
                .items(createOrderRequest.getItems().stream().map(item ->
                        RestaurantValidationRequest.Item.builder()
                                .productId(item.getProductId())
                                .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .build()).toList())
                .totalPrice(createOrderRequest.getPrice())
                .build();
    }

}
