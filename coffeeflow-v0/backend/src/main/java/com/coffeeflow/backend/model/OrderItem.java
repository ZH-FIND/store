package com.coffeeflow.backend.model;

import java.math.BigDecimal;

/**
 * 订单明细：单价与小计是下单时刻的快照，不随商品表后续改价而变化。
 * size（小杯/中杯/大杯）仅为展示属性，不参与任何金额计算。
 */
public class OrderItem {
    private final int lineNo;
    private final String productCode;
    private final String drinkName;
    private final String size;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal subtotal;

    public OrderItem(int lineNo, String productCode, String drinkName, String size,
                     int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
        this.lineNo = lineNo;
        this.productCode = productCode;
        this.drinkName = drinkName;
        this.size = size;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public int getLineNo() { return lineNo; }
    public String getProductCode() { return productCode; }
    public String getDrinkName() { return drinkName; }
    public String getSize() { return size; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
}
