package com.coffeeflow.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class OrderSummaryResponse {
    private final String id;
    private final String storeName;
    private final String customerName;
    private final String productCode;
    private final String drinkName;
    private final List<String> items;
    private final int quantity;
    private final String status;
    private final LocalDateTime createdAt;
    private final LocalDateTime estimatedReadyAt;
    private final String note;

    public OrderSummaryResponse(String id, String storeName, String customerName,
            String productCode, String drinkName, List<String> items, int quantity,
            String status, LocalDateTime createdAt, LocalDateTime estimatedReadyAt, String note) {
        this.id = id;
        this.storeName = storeName;
        this.customerName = customerName;
        this.productCode = productCode;
        this.drinkName = drinkName;
        this.items = items;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
        this.estimatedReadyAt = estimatedReadyAt;
        this.note = note;
    }

    public String getId() { return id; }
    public String getStoreName() { return storeName; }
    public String getCustomerName() { return customerName; }
    public String getProductCode() { return productCode; }
    public String getDrinkName() { return drinkName; }
    public List<String> getItems() { return items; }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getEstimatedReadyAt() { return estimatedReadyAt; }
    public String getNote() { return note; }
}
