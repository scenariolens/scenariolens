package com.example.phantom;

import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final InventoryRepository inventoryRepository;
    private final PaymentClient paymentClient;

    public OrderService(InventoryRepository inventoryRepository, PaymentClient paymentClient) {
        this.inventoryRepository = inventoryRepository;
        this.paymentClient = paymentClient;
    }

    public String processOrder(String itemId, double amount) {
        try {
            // Dependency 1: Database call
            inventoryRepository.reserveItem(itemId); 
            
            // Dependency 2: REST Client call
            paymentClient.chargeCard(amount);        
            
            return "ORDER_SUCCESS";
            
        } catch (RuntimeException e) {
            // Shared exception pathway
            return "ORDER_FAILED_DUE_TO_ERROR";
        }
    }
}

// --- Dependencies (Usually in separate files) ---

interface InventoryRepository {
    void reserveItem(String itemId);
}

interface PaymentClient {
    void chargeCard(double amount);
}
