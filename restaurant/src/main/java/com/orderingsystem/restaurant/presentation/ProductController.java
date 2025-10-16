package com.orderingsystem.restaurant.presentation;

import com.orderingsystem.restaurant.application.FindProductService;
import com.orderingsystem.restaurant.application.ProductFacade;
import com.orderingsystem.restaurant.application.dto.response.ProductResponse;
import com.orderingsystem.restaurant.presentation.request.CreateProductRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/products")
@RequiredArgsConstructor
public class ProductController {

    private final RestaurantControllerHelper restaurantControllerHelper;
    private final ProductFacade productFacade;
    private final FindProductService findProductService;

    @PostMapping
    public ResponseEntity<Void> createProduct(@Valid @RequestBody CreateProductRequest createProductRequest,
                                              BindingResult bindingResult,
                                              @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                              @PathVariable UUID restaurantId) {
        UUID restaurantOwnerId = restaurantControllerHelper.getRestaurantOwnerId(authorizationHeader);
        RestaurantControllerHelper.valid(bindingResult);
        UUID productId = productFacade.create(
                createProductRequest.toApplicationRequest(), restaurantOwnerId, restaurantId);
        return ResponseEntity.created(URI.create("/api/restaurants/" + restaurantId + "/products/" + productId)).build();
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(@PathVariable UUID restaurantId,
                                                             @PageableDefault(size = 10, sort = "createdAt", direction = Direction.DESC) Pageable pageable){
        return ResponseEntity.ok(findProductService.findAll(restaurantId, pageable));
    }

}
