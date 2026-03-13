package com.shopsphere.payment.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.shopsphere.payment.client.OrderClient;
import com.shopsphere.payment.dto.request.InitiatePaymentRequest;
import com.shopsphere.payment.dto.response.InitiatePaymentResponse;
import com.shopsphere.payment.dto.response.PaymentResponse;
import com.shopsphere.payment.entity.Payment;
import com.shopsphere.payment.enums.PaymentMethod;
import com.shopsphere.payment.enums.PaymentStatus;
import com.shopsphere.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;
    private final RazorpayClient razorpayClient;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // ── Called by OrderService via Feign ──────────────────────────
    @Transactional
    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest req) {
        if (req.getPaymentMethod() == PaymentMethod.COD) {
            return handleCod(req);
        } else {
            return handleRazorpay(req);
        }
    }

    private InitiatePaymentResponse handleCod(InitiatePaymentRequest req) {
        Payment payment = Payment.builder()
                .orderId(req.getOrderId())
                .userId(req.getUserId())
                .amount(req.getAmount())
                .paymentMethod(PaymentMethod.COD)
                .status(PaymentStatus.COMPLETED)
                .build();

        paymentRepository.save(payment);

        // Immediately confirm order for COD
        try {
            orderClient.confirmOrder(req.getOrderId());
            log.info("Order {} confirmed for COD payment", req.getOrderId());
        } catch (Exception e) {
            log.error("Failed to confirm COD order {}: {}", req.getOrderId(), e.getMessage());
            // Don't fail payment — order confirmation is best-effort here
            // A reconciliation job would fix this in production
        }

        return InitiatePaymentResponse.builder()
                .paymentId(payment.getId())
                .status(PaymentStatus.COMPLETED)
                .paymentMethod(PaymentMethod.COD)
                .build();
    }

    private InitiatePaymentResponse handleRazorpay(InitiatePaymentRequest req) {
        try {
            JSONObject options = new JSONObject();
            // Razorpay amount is in paise (1 INR = 100 paise)
            options.put("amount", req.getAmount().multiply(BigDecimal.valueOf(100)).intValue());
            options.put("currency", "INR");
            options.put("receipt", req.getOrderId().toString().replace("-", ""));

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(options);
            String razorpayOrderId = razorpayOrder.get("id");

            Payment payment = Payment.builder()
                    .orderId(req.getOrderId())
                    .userId(req.getUserId())
                    .amount(req.getAmount())
                    .paymentMethod(PaymentMethod.RAZORPAY)
                    .status(PaymentStatus.PENDING)
                    .razorpayOrderId(razorpayOrderId)
                    .build();

            paymentRepository.save(payment);

            return InitiatePaymentResponse.builder()
                    .paymentId(payment.getId())
                    .status(PaymentStatus.PENDING)
                    .paymentMethod(PaymentMethod.RAZORPAY)
                    .razorpayOrderId(razorpayOrderId)
                    .razorpayKeyId(razorpayKeyId)
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new RuntimeException("Payment initiation failed: " + e.getMessage());
        }
    }

    // ── Called by Razorpay webhook ────────────────────────────────
    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (!verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid Razorpay webhook signature");
            throw new SecurityException("Invalid webhook signature");
        }

        JSONObject event = new JSONObject(payload);
        String eventType = event.getString("event");

        if ("payment.captured".equals(eventType)) {
            JSONObject paymentEntity = event
                    .getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String razorpayOrderId   = paymentEntity.getString("order_id");
            String razorpayPaymentId = paymentEntity.getString("id");
            String razorpaySignature = signature;

            paymentRepository.findAll().stream()
                    .filter(p -> razorpayOrderId.equals(p.getRazorpayOrderId()))
                    .findFirst()
                    .ifPresent(payment -> {
                        payment.setStatus(PaymentStatus.COMPLETED);
                        payment.setRazorpayPaymentId(razorpayPaymentId);
                        payment.setRazorpaySignature(razorpaySignature);
                        paymentRepository.save(payment);

                        try {
                            orderClient.confirmOrder(payment.getOrderId());
                            log.info("Order {} confirmed after Razorpay payment", payment.getOrderId());
                        } catch (Exception e) {
                            log.error("Failed to confirm order {}: {}", payment.getOrderId(), e.getMessage());
                        }
                    });
        }
    }

    private boolean verifyWebhookSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes());
            String computed = HexFormat.of().formatHex(hash);
            return computed.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    // ── Queries ───────────────────────────────────────────────────
    public PaymentResponse getByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrderId())
                .userId(p.getUserId())
                .amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod())
                .status(p.getStatus())
                .razorpayOrderId(p.getRazorpayOrderId())
                .razorpayPaymentId(p.getRazorpayPaymentId())
                .createdAt(p.getCreatedAt())
                .build();
    }
}