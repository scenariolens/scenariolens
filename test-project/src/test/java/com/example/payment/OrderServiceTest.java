package com.example.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void testProcessOrder() {
        when(orderRepository.findById(any())).thenReturn(Order.builder().status("PENDING").build());
        when(inventoryService.reserve(any())).thenReturn(true);
        orderService.processOrder("123");
        verify(notificationClient).send(any());
    }

    @Test
    void testCancelOrder() {
        when(orderRepository.findById(any())).thenReturn(Order.builder().status("PENDING").build());
        orderService.cancelOrder("123");
        verify(inventoryService).release(any());
    }

    @Test
    void testCheckStatus() {
        when(orderRepository.findById(any())).thenReturn(Order.builder().status("PENDING").build());
        when(inventoryService.reserve(any())).thenReturn(true);
        orderService.checkStatus("123");
    }
}
