package com.shopsphere.product.messaging;

import com.shopsphere.common.dto.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.product-created}")
    private String productCreatedKey;

    @Value("${app.rabbitmq.routing-key.product-updated}")
    private String productUpdatedKey;

    @Value("${app.rabbitmq.routing-key.product-deleted}")
    private String productDeletedKey;

    public void publishProductCreated(ProductEvent event) {
        publish(productCreatedKey, event);
    }

    public void publishProductUpdated(ProductEvent event) {
        publish(productUpdatedKey, event);
    }

    public void publishProductDeleted(ProductEvent event) {
        publish(productDeletedKey, event);
    }

    private void publish(String routingKey, ProductEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Published {} for product {}", event.getEventType(), event.getProductId());
        } catch (Exception e) {
            log.error("Failed to publish {} for product {}: {}",
                    event.getEventType(), event.getProductId(), e.getMessage());
            // Never break product flow
        }
    }
}