package com.orderingsystem.order.infra.api;

import com.orderingsystem.order.application.RestaurantApi;
import com.orderingsystem.order.application.dto.RestaurantInfo;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class RestaurantApiImpl implements RestaurantApi {

    private final WebClient webClient;

    public RestaurantApiImpl(@Value("${service.url}") String serviceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(serviceUrl)
                .build();
    }

    @Override
    public RestaurantInfo getRestaurantInfo(UUID restaurantId, List<UUID> productIds) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("restaurant/{restaurantId}/products")
                        .queryParam("productIds", productIds)
                        .build(restaurantId))
                .retrieve()
                .bodyToMono(RestaurantInfo.class)
                .block();
    }
}
