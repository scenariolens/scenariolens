package com.example.payment;

class OrderNotification {
}

interface NotificationClient {
    void send(OrderNotification notification);
}

interface InventoryService {
    boolean reserve(String orderId);
    void release(String orderId);
}

public class OrderService {

    private final OrderRepository orderRepository;
    private final NotificationClient notificationClient;
    private final InventoryService inventoryService;

    public OrderService(OrderRepository orderRepository, NotificationClient notificationClient, InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.notificationClient = notificationClient;
        this.inventoryService = inventoryService;
    }

    public void processOrder(String orderId) {
        Order order = orderRepository.findById(orderId);
        if (order != null) {
            boolean reserved = inventoryService.reserve(orderId);
            if (reserved) {
                notificationClient.send(new OrderNotification());
            }
        }
    }

    public void cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId);
        if (order != null) {
            inventoryService.release(orderId);
            notificationClient.send(new OrderNotification());
        }
    }

    public boolean checkStatus(String orderId) {
        Order order = orderRepository.findById(orderId);
        if (order != null) {
            return inventoryService.reserve(orderId); // just for using 2 deps
        }
        return false;
    }
}
