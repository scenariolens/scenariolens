package com.example.payment;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
class Order {
    private String id;
    private String status;
}

@Data
@Builder
class RefundRequest {
    private String orderId;
    private double amount;
}

@Data
@Builder
class PaymentResponse {
    private boolean success;
    private String errorCode;
}

@Data
@Builder
class RefundResponse {
    private String status;
    private String errorCode;

    public static RefundResponse success() {
        return RefundResponse.builder().status("SUCCESS").build();
    }
    public static RefundResponse failed(String errorCode) {
        return RefundResponse.builder().status("FAILED").errorCode(errorCode).build();
    }
}

class OrderNotFoundException extends RuntimeException {}
class InvalidStateException extends RuntimeException {}
class PaymentException extends Exception {
    private String code;
    public String getCode() { return code; }
    public PaymentException(String message, String code) { super(message); this.code = code; }
}
class ServiceException extends RuntimeException {
    public ServiceException(Throwable cause) { super(cause); }
}

enum RefundPolicy {
    PREMIUM, STANDARD, HOLD
}

@Data
@Builder
class TransferRequest {
    private String sourceId;
    private String targetId;
}

class TransferResponse {
    public static TransferResponse from(PaymentResponse response) { return new TransferResponse(); }
}

interface OrderRepository {
    Order findById(String id);
}

interface PaymentClient {
    PaymentResponse refund(double amount) throws PaymentException;
    PaymentResponse transfer(Order source, Order target);
}

interface EventPublisher {
    void publish(Object event);
}

interface AuditLogger {
    void log(String event, String message);
}

interface PolicyService {
    RefundPolicy getPremiumPolicy();
    RefundPolicy getStandardPolicy();
}

public class PaymentService {
    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;
    private final EventPublisher eventPublisher;
    private final AuditLogger auditLogger;
    private final PolicyService policyService;

    public PaymentService(OrderRepository orderRepository, PaymentClient paymentClient, EventPublisher eventPublisher, AuditLogger auditLogger, PolicyService policyService) {
        this.orderRepository = orderRepository;
        this.paymentClient = paymentClient;
        this.eventPublisher = eventPublisher;
        this.auditLogger = auditLogger;
        this.policyService = policyService;
    }

    public RefundResponse processRefund(RefundRequest request) {
        Order order = orderRepository.findById(request.getOrderId());
        if (order == null) throw new OrderNotFoundException();
        if (!"DELIVERED".equals(order.getStatus())) throw new InvalidStateException();

        try {
            PaymentResponse payment = paymentClient.refund(request.getAmount());
            if (payment.isSuccess()) {
                eventPublisher.publish("RefundEvent");
                return RefundResponse.success();
            }
            return RefundResponse.failed(payment.getErrorCode());
        } catch (PaymentException e) {
            return RefundResponse.failed(e.getCode());
        }
    }

    public RefundResponse processRefundWithRetry(RefundRequest request) {
        Order order = orderRepository.findById(request.getOrderId());
        if (order == null) throw new OrderNotFoundException();

        try {
            PaymentResponse payment = paymentClient.refund(request.getAmount());
            return RefundResponse.success();
        } catch (PaymentException e) {
            auditLogger.log("REFUND_FAILED", e.getMessage());
            return RefundResponse.failed(e.getCode());
        } catch (RuntimeException e) {
            throw new ServiceException(e);
        }
    }

    public RefundPolicy getRefundPolicy(Order order) {
        switch (order.getStatus()) {
            case "DELIVERED":   return policyService.getPremiumPolicy();
            case "CANCELLED":   return policyService.getStandardPolicy();
            case "PENDING":     return RefundPolicy.HOLD;
            default:          throw new InvalidStateException();
        }
    }

    public boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public TransferResponse transfer(TransferRequest request) {
        Order source = orderRepository.findById(request.getSourceId());
        Order target = orderRepository.findById(request.getTargetId());
        if (source == null || target == null) throw new OrderNotFoundException();
        PaymentResponse payment = paymentClient.transfer(source, target);
        return TransferResponse.from(payment);
    }
}
