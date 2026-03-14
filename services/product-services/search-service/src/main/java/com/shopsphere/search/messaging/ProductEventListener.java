package com.shopsphere.search.messaging;

import com.shopsphere.common.dto.ProductEvent;
import com.shopsphere.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventListener {

    private final SearchService searchService;

    @RabbitListener(queues = "${app.rabbitmq.queue.search}")
    public void onProductEvent(ProductEvent event) {
        log.info("Received {} for product {}", event.getEventType(), event.getProductId());

        switch (event.getEventType()) {
            case PRODUCT_CREATED, PRODUCT_UPDATED -> searchService.indexProduct(event);
            case PRODUCT_DELETED                  -> searchService.deleteFromIndex(event);
        }
    }
}