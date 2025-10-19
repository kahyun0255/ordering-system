package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.dto.request.UpdateProductApplicationRequest;
import com.orderingsystem.restaurant.application.dto.response.ProductResponse;
import com.orderingsystem.restaurant.domain.exception.ProductNotFoundException;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse updateProduct(UUID ownerId, UUID restaurantId, UUID productId,
                                         UpdateProductApplicationRequest request) {
        Product product = getProduct(productId);

        log.info("{} 유저가 {} 레스토랑의 {} 상품 정보를 변경했습니다. before = {}, after = {}",
                ownerId, restaurantId, productId, product.toString(), request.toString());

        update(request, product);
        return ProductResponse.from(product);
    }

    private Product getProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("상품 정보를 찾을 수 없습니다."));
    }

    private void update(UpdateProductApplicationRequest request, Product product) {
        if (request.getName() != null && !request.getName().equals(product.getName())) {
            product.updateName(request.getName());
        }

        if (request.getPrice() != null && request.getPrice().compareTo(product.getPrice().getAmount()) != 0) {
            product.updatePrice(request.getPrice());
        }

        if (request.getAvailable() != null && !request.getAvailable().equals(product.isAvailable())) {
            product.updateAvailability(request.getAvailable());
        }

        if (request.getQuantity() != null && !request.getQuantity().equals(product.getQuantity())) {
            product.updateQuantity(request.getQuantity());
        }
    }

}
