package com.orderingsystem.restaurant.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.restaurant.domain.repository.OrderApprovalRepository;
import com.orderingsystem.restaurant.domain.repository.OwnerRepository;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantOwnershipRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantProductRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import com.orderingsystem.restaurant.domain.repository.outbox.OrderOutboxRepository;
import com.orderingsystem.restaurant.domain.repository.outbox.ProcessedMessageRepository;
import com.orderingsystem.restaurant.domain.repository.outbox.RestaurantUpdateOutboxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public abstract class ApplicationTestSupport {

    @Autowired
    protected RestaurantUpdateService restaurantUpdateService;

    @Autowired
    protected RestaurantOwnershipRepository restaurantOwnershipRepository;

    @Autowired
    protected OwnerRepository ownerRepository;

    @Autowired
    protected RestaurantRepository restaurantRepository;

    @Autowired
    protected OrderApprovalRepository orderApprovalRepository;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected RestaurantProductRepository restaurantProductRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected OrderOutboxRepository orderOutboxRepository;

    @Autowired
    protected RestaurantUpdateOutboxRepository restaurantUpdateOutboxRepository;

    @Autowired
    protected ProcessedMessageRepository processedMessageRepository;

}
