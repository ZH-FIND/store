package com.coffeeflow.backend.model;

import java.math.BigDecimal;

/**
 * 商品（目录视角）。
 * available 只在「门店 × 商品」视角下有意义，纯商品目录查询时为 null。
 */
public class Product {
    private final String productCode;
    private final String name;
    private final BigDecimal price;
    private final Boolean available;

    public Product(String productCode, String name, BigDecimal price) {
        this(productCode, name, price, null);
    }

    public Product(String productCode, String name, BigDecimal price, Boolean available) {
        this.productCode = productCode;
        this.name = name;
        this.price = price;
        this.available = available;
    }

    public String getProductCode() { return productCode; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public Boolean getAvailable() { return available; }
}
