package com.coffeeflow.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class OrderDetailResponse extends OrderSummaryResponse {
    private final String size;

    public OrderDetailResponse(String id, String storeName, String customerName,
            String productCode, String drinkName, List<String> items, String size,
            int quantity, String status, LocalDateTime createdAt,
            LocalDateTime estimatedReadyAt, String note) {
        super(id, storeName, customerName, productCode, drinkName, items, quantity,
                status, createdAt, estimatedReadyAt, note);
        this.size = size;
    }

    public String getSize() { return size; }
}
