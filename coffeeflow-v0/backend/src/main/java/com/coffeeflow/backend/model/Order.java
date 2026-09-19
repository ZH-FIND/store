package com.coffeeflow.backend.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order {
    private String id;
    private String storeName;
    private String customerName;
    private String productCode;
    private String drinkName;
    private String size;
    private int quantity;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime estimatedReadyAt;
    private List<String> items = new ArrayList<>();
    private String note;

    public Order(String id, String storeName, String customerName, String productCode, String drinkName,
                 String size, int quantity, OrderStatus status, LocalDateTime createdAt,
                 LocalDateTime estimatedReadyAt, List<String> items, String note) {
        this.id = id;
        this.storeName = storeName;
        this.customerName = customerName;
        this.productCode = productCode;
        this.drinkName = drinkName;
        this.size = size;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
        this.estimatedReadyAt = estimatedReadyAt;
        this.items = new ArrayList<>(items);
        this.note = note;
    }

    public String getId() { return id; }
    public String getStoreName() { return storeName; }
    public String getCustomerName() { return customerName; }
    public String getProductCode() { return productCode; }
    public String getDrinkName() { return drinkName; }
    public String getSize() { return size; }
    public int getQuantity() { return quantity; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getEstimatedReadyAt() { return estimatedReadyAt; }
    public List<String> getItems() { return Collections.unmodifiableList(items); }
    public String getNote() { return note; }
}
