package com.coffeeflow.backend.model;

public class Store {
    private final String storeId;
    private final String storeName;

    public Store(String storeId, String storeName) {
        this.storeId = storeId;
        this.storeName = storeName;
    }

    public String getStoreId() { return storeId; }
    public String getStoreName() { return storeName; }
}
