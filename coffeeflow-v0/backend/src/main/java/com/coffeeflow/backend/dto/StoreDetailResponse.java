package com.coffeeflow.backend.dto;

public class StoreDetailResponse {
    private final String storeId;
    private final String storeName;

    public StoreDetailResponse(String storeId, String storeName) {
        this.storeId = storeId;
        this.storeName = storeName;
    }

    public String getStoreId() { return storeId; }
    public String getStoreName() { return storeName; }
}
