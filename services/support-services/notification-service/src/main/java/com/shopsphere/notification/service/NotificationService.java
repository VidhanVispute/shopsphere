package com.shopsphere.notification.service;

import com.shopsphere.common.dto.NotificationEvent;
import com.shopsphere.notification.entity.Notification;
import com.shopsphere.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EmailService emailService;
    private final NotificationRepository notificationRepository;

    public void processEvent(NotificationEvent event) {
        String subject = buildSubject(event);
        String body    = buildBody(event);

        Notification notification = Notification.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .userEmail(event.getUserEmail())
                .eventType(event.getEventType().name())
                .subject(subject)
                .body(body)
                .status("SENT")
                .build();

        try {
            emailService.sendEmail(event.getUserEmail(), subject, body);
            log.info("Email sent to {} for event {}", event.getUserEmail(), event.getEventType());
        } catch (Exception e) {
            log.error("Failed to send email for event {}: {}", event.getEventType(), e.getMessage());
            notification.setStatus("FAILED");
            notification.setErrorMessage(e.getMessage());
        }

        notificationRepository.save(notification);
    }

    private String buildSubject(NotificationEvent event) {
        return switch (event.getEventType()) {
            case ORDER_PLACED    -> "Order Placed — ShopSphere #" + event.getOrderId().toString().substring(0, 8).toUpperCase();
            case ORDER_CONFIRMED -> "Payment Confirmed — Your order is being processed!";
            case ORDER_CANCELLED -> "Order Cancelled — ShopSphere";
        };
    }

    private String buildBody(NotificationEvent event) {
        String orderId = event.getOrderId().toString().substring(0, 8).toUpperCase();
        return switch (event.getEventType()) {
            case ORDER_PLACED -> """
                    <h2>Thank you for your order! 🎉</h2>
                    <p>Your order <strong>#%s</strong> has been placed successfully.</p>
                    <p><strong>Amount:</strong> %s</p>
                    <p><strong>Payment Method:</strong> %s</p>
                    <p>We'll notify you once your payment is confirmed.</p>
                    <br><p>— Team ShopSphere</p>
                    """.formatted(orderId, event.getOrderAmount(), event.getPaymentMethod());

            case ORDER_CONFIRMED -> """
                    <h2>Payment Confirmed! ✅</h2>
                    <p>Great news! Your order <strong>#%s</strong> has been confirmed.</p>
                    <p><strong>Amount Paid:</strong> %s</p>
                    <p>Your order is now being processed and will be shipped soon.</p>
                    <br><p>— Team ShopSphere</p>
                    """.formatted(orderId, event.getOrderAmount());

            case ORDER_CANCELLED -> """
                    <h2>Order Cancelled</h2>
                    <p>Your order <strong>#%s</strong> has been cancelled.</p>
                    <p><strong>Amount:</strong> %s</p>
                    <p>If you did not request this cancellation, please contact support.</p>
                    <br><p>— Team ShopSphere</p>
                    """.formatted(orderId, event.getOrderAmount());
        };
    }
}