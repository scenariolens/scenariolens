package com.example.phantom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    void testProcessOrder_HappyPath_BothSucceed() {
        // Act
        String result = orderService.processOrder("ITEM-123", 100.0);

        // Assert
        assertEquals("ORDER_SUCCESS", result);
        verify(inventoryRepository).reserveItem("ITEM-123");
        verify(paymentClient).chargeCard(100.0);
    }

    @Test
    void testProcessOrder_InventoryThrowsException_CaughtByCatchBlock() {
        // Arrange: Simulate Database failure
        doThrow(new RuntimeException("DB Connection Lost"))
            .when(inventoryRepository).reserveItem(anyString());

        // Act
        String result = orderService.processOrder("ITEM-123", 100.0);

        // Assert
        assertEquals("ORDER_FAILED_DUE_TO_ERROR", result);
        verify(inventoryRepository).reserveItem("ITEM-123");
        verifyNoInteractions(paymentClient); // Payment is never reached
    }

    @Test
    void testProcessOrder_PaymentClientThrowsException_CaughtByCatchBlock() {
        // Arrange: Simulate Payment REST call failure (ScenarioLens Gap S02)
        doNothing().when(inventoryRepository).reserveItem(anyString());
        doThrow(new RuntimeException("Payment Gateway Timeout"))
            .when(paymentClient).chargeCard(anyDouble());

        // Act
        String result = orderService.processOrder("ITEM-123", 100.0);

        // Assert
        assertEquals("ORDER_FAILED_DUE_TO_ERROR", result);
        verify(inventoryRepository).reserveItem("ITEM-123");
        verify(paymentClient).chargeCard(100.0);
    }
}
