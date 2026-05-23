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
    PaymentResponse refund(Order order) throws PaymentException;
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
    private final ServiceA serviceA;
    private final ServiceB serviceB;
    private final ServiceC serviceC;
    private final ServiceD serviceD;
    private final ServiceE serviceE;
    private final ServiceF serviceF;
    private final ServiceG serviceG;
    private final ServiceH serviceH;
    private final java.util.concurrent.ExecutorService executor;

    public static final String CREATED = "CREATED";
    public static final String PENDING = "PENDING";
    public static final String PROCESSING = "PROCESSING";
    public static final String DELIVERED = "DELIVERED";
    public static final String CANCELLED = "CANCELLED";
    public static final String RETURNED = "RETURNED";
    public static final String DISPUTED = "DISPUTED";
    public static final String REFUNDED = "REFUNDED";

    public PaymentService(OrderRepository orderRepository, PaymentClient paymentClient, EventPublisher eventPublisher, AuditLogger auditLogger, PolicyService policyService) {
        this(orderRepository, paymentClient, eventPublisher, auditLogger, policyService, null, null, null, null, null, null, null, null, null);
    }

    public PaymentService(OrderRepository orderRepository, PaymentClient paymentClient, EventPublisher eventPublisher, AuditLogger auditLogger, PolicyService policyService,
                          ServiceA serviceA, ServiceB serviceB, ServiceC serviceC, ServiceD serviceD, ServiceE serviceE, ServiceF serviceF, ServiceG serviceG, ServiceH serviceH,
                          java.util.concurrent.ExecutorService executor) {
        this.orderRepository = orderRepository;
        this.paymentClient = paymentClient;
        this.eventPublisher = eventPublisher;
        this.auditLogger = auditLogger;
        this.policyService = policyService;
        this.serviceA = serviceA;
        this.serviceB = serviceB;
        this.serviceC = serviceC;
        this.serviceD = serviceD;
        this.serviceE = serviceE;
        this.serviceF = serviceF;
        this.serviceG = serviceG;
        this.serviceH = serviceH;
        this.executor = executor;
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

    // --- SYNTHETIC PATHOLOGICAL METHODS ---

    public Response deeplyNested(Request req) {
        A a = serviceA.get(req.getId());
        if (a == null) throw new AException();
        B b = serviceB.get(a.getBId());
        if (b == null) throw new BException();
        C c = serviceC.get(b.getCId());
        if (c == null) throw new CException();
        D d = serviceD.get(c.getDId());
        if (d == null) throw new DException();
        E e = serviceE.get(d.getEId());
        if (e == null) throw new EException();
        return serviceF.process(a, b, c, d, e);
    }

    public Response manyStatuses(Order order) {
        switch (order.getStatus()) {
            case CREATED:    return serviceA.handleCreated(order);
            case PENDING:    return serviceB.handlePending(order);
            case PROCESSING: return serviceC.handleProcessing(order);
            case DELIVERED:  return serviceD.handleDelivered(order);
            case CANCELLED:  return serviceE.handleCancelled(order);
            case RETURNED:   return serviceF.handleReturned(order);
            case DISPUTED:   return serviceG.handleDisputed(order);
            case REFUNDED:   return serviceH.handleRefunded(order);
            default:         throw new UnknownStatusException();
        }
    }

    public Response nestedTryCatch(Request req) {
        try {
            Order order = orderRepository.findById(req.getId());
            try {
                PaymentResponse payment = paymentClient.refund(order);
                try {
                    eventPublisher.publish(new Event(order, payment));
                    return Response.success();
                } catch (PublishException e) {
                    auditLogger.log("PUBLISH_FAILED", e.getMessage());
                    return Response.partialSuccess();
                }
            } catch (PaymentException e) {
                auditLogger.log("PAYMENT_FAILED", e.getCode());
                return Response.failed(e.getCode());
            }
        } catch (OrderException e) {
            return Response.notFound();
        }
    }

    public Response manyDependencies(Request req) {
        A a = serviceA.call(req);
        B b = serviceB.call(req);
        C c = serviceC.call(req);
        D d = serviceD.call(req);
        if (a == null || b == null || c == null || d == null) {
            throw new ValidationException();
        }
        return serviceE.process(a, b, c, d);
    }

    // --- MALFORMED & EDGE RESILIENCE METHODS ---

    public void empty() {}

    public String commentOnly() {
        // Comment
        return "constant";
    }

    @SuppressWarnings("unchecked")
    public <T extends Response> T generic(Request req) {
        return (T) serviceA.process(req);
    }

    public Response withAnonymous(Request req) {
        Runnable r = new Runnable() {
            public void run() { serviceA.doSomething(); }
        };
        executor.submit(r);
        return Response.success();
    }
}

// --- STRESS-TEST SUPPORTING CLASSES & INTERFACES ---

class Request {
    private String id;
    public String getId() { return id; }
}

class Response {
    private String status;
    private String errorCode;
    public String getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public static Response success() { return new Response(); }
    public static Response partialSuccess() { return new Response(); }
    public static Response failed(String c) { return new Response(); }
    public static Response notFound() { return new Response(); }
}

class A {
    public String getBId() { return ""; }
}
class B {
    public String getCId() { return ""; }
}
class C {
    public String getDId() { return ""; }
}
class D {
    public String getEId() { return ""; }
}
class E {}

class AException extends RuntimeException {}
class BException extends RuntimeException {}
class CException extends RuntimeException {}
class DException extends RuntimeException {}
class EException extends RuntimeException {}
class UnknownStatusException extends RuntimeException {}
class PublishException extends RuntimeException {}
class OrderException extends RuntimeException {}
class ValidationException extends RuntimeException {}

class Event {
    public Event(Order o, PaymentResponse p) {}
}

interface ServiceA {
    A get(String id);
    A call(Request r);
    Response process(Request r);
    void doSomething();
    Response handleCreated(Order o);
}
interface ServiceB {
    B get(String id);
    B call(Request r);
    Response handlePending(Order o);
}
interface ServiceC {
    C get(String id);
    C call(Request r);
    Response handleProcessing(Order o);
}
interface ServiceD {
    D get(String id);
    D call(Request r);
    Response handleDelivered(Order o);
}
interface ServiceE {
    E get(String id);
    Response process(A a, B b, C c, D d);
    Response handleCancelled(Order o);
}
interface ServiceF {
    Response process(A a, B b, C c, D d, E e);
    Response handleReturned(Order o);
}
interface ServiceG {
    Response handleDisputed(Order o);
}
interface ServiceH {
    Response handleRefunded(Order o);
}
