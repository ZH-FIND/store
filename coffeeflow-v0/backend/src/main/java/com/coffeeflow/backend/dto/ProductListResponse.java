package com.coffeeflow.backend.dto;

import java.util.List;

public class ProductListResponse {
    private final int total;
    private final List<ProductDetailResponse> products;

    public ProductListResponse(int total, List<ProductDetailResponse> products) {
        this.total = total;
        this.products = products;
    }

    public int getTotal() { return total; }
    public List<ProductDetailResponse> getProducts() { return products; }
}
