package com.example.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private PolicyService policyService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void testSuccess() throws PaymentException {
        when(orderRepository.findById(any())).thenReturn(Order.builder().status("DELIVERED").build());
        when(paymentClient.refund(anyDouble())).thenReturn(PaymentResponse.builder().success(true).build());

        RefundResponse response = paymentService.processRefund(RefundRequest.builder().orderId("123").amount(100.0).build());

        assertEquals("SUCCESS", response.getStatus());
        verify(eventPublisher).publish(any());
    }

    @Test
    void testOrderNotFound() {
        when(orderRepository.findById(any())).thenReturn(null);

        assertThrows(OrderNotFoundException.class, () -> 
            paymentService.processRefund(RefundRequest.builder().orderId("123").amount(100.0).build())
        );
    }

    @Test
    void testProcessRefundWithRetrySuccess() throws PaymentException {
        when(orderRepository.findById(any())).thenReturn(Order.builder().status("DELIVERED").build());
        when(paymentClient.refund(anyDouble())).thenReturn(PaymentResponse.builder().success(true).build());

        RefundResponse response = paymentService.processRefundWithRetry(RefundRequest.builder().orderId("123").amount(100.0).build());

        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void testGetRefundPolicy() {
        when(policyService.getPremiumPolicy()).thenReturn(RefundPolicy.PREMIUM);
        RefundPolicy policy = paymentService.getRefundPolicy(Order.builder().status("DELIVERED").build());
        assertEquals(RefundPolicy.PREMIUM, policy);
    }

    @Test
    void testIsValidAmount() {
        assertTrue(paymentService.isValidAmount(java.math.BigDecimal.TEN));
    }

    @Test
    void testTransfer() {
        when(orderRepository.findById(any())).thenReturn(Order.builder().status("DELIVERED").build());
        when(paymentClient.transfer(any(), any())).thenReturn(PaymentResponse.builder().success(true).build());
        TransferResponse response = paymentService.transfer(TransferRequest.builder().sourceId("1").targetId("2").build());
        assertNotNull(response);
    }
}
