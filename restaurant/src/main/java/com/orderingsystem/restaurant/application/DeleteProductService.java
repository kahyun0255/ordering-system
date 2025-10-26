package com.orderingsystem.restaurant.application;

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
public class DeleteProductService {

    private final ProductRepository productRepository;

    @Transactional
    public void deleteProduct(UUID restaurantId, UUID productId, UUID restaurantOwnerId) {
        log.info("{} 유저가 {} 레스토랑의 {} 상품을 삭제했습니다.", restaurantOwnerId, restaurantId, productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("상품 정보를 찾을 수 없습니다."));

        product.delete();
    }

}
