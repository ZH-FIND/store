package com.coffeeflow.backend.dto;

import java.util.List;

public class StoreListResponse {
    private final int total;
    private final List<StoreDetailResponse> stores;

    public StoreListResponse(int total, List<StoreDetailResponse> stores) {
        this.total = total;
        this.stores = stores;
    }

    public int getTotal() { return total; }
    public List<StoreDetailResponse> getStores() { return stores; }
}
