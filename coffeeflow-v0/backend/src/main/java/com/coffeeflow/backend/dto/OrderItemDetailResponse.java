package com.coffeeflow.backend.dto;

import java.math.BigDecimal;

public class OrderItemDetailResponse {
    private final String productCode;
    private final String drinkName;
    private final String size;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal subtotal;

    public OrderItemDetailResponse(String productCode, String drinkName, String size, int quantity,
            BigDecimal unitPrice, BigDecimal subtotal) {
        this.productCode = productCode;
        this.drinkName = drinkName;
        this.size = size;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public String getProductCode() { return productCode; }
    public String getDrinkName() { return drinkName; }
    public String getSize() { return size; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
}
