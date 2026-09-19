package com.coffeeflow.backend.service;

import com.coffeeflow.backend.dto.ProductDetailResponse;
import com.coffeeflow.backend.dto.ProductListResponse;
import com.coffeeflow.backend.dto.StoreListResponse;

public interface StoreService {
    StoreListResponse getStores();

    ProductListResponse getStoreProducts(String storeId);

    ProductDetailResponse updateProductAvailability(String storeId, String productCode, boolean available);
}
