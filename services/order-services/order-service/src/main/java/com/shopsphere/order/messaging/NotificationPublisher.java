package com.shopsphere.order.messaging;

import com.shopsphere.common.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.order-placed}")
    private String orderPlacedKey;

    @Value("${app.rabbitmq.routing-key.order-confirmed}")
    private String orderConfirmedKey;

    @Value("${app.rabbitmq.routing-key.order-cancelled}")
    private String orderCancelledKey;

    public void publishOrderPlaced(NotificationEvent event) {
        publish(orderPlacedKey, event);
    }

    public void publishOrderConfirmed(NotificationEvent event) {
        publish(orderConfirmedKey, event);
    }

    public void publishOrderCancelled(NotificationEvent event) {
        publish(orderCancelledKey, event);
    }

    private void publish(String routingKey, NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Published {} event for order {}", event.getEventType(), event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to publish {} event for order {}: {}",
                    event.getEventType(), event.getOrderId(), e.getMessage());
            // Notification failure must NEVER break the order flow
        }
    }
}