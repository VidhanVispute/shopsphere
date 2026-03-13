package com.shopsphere.notification.messaging;

import com.shopsphere.common.dto.NotificationEvent;
import com.shopsphere.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${app.rabbitmq.queue.notification}")
    public void onNotificationEvent(NotificationEvent event) {
        log.info("Received {} event for order {}", event.getEventType(), event.getOrderId());
        notificationService.processEvent(event);
    }
}