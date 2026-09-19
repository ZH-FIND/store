package com.coffeeflow.backend.controller;

import com.coffeeflow.backend.dto.ProductDetailResponse;
import com.coffeeflow.backend.dto.ProductListResponse;
import com.coffeeflow.backend.dto.StoreListResponse;
import com.coffeeflow.backend.dto.UpdateProductAvailabilityRequest;
import com.coffeeflow.backend.service.StoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {
    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public StoreListResponse getStores() {
        return storeService.getStores();
    }

    @GetMapping("/{storeId}/products")
    public ProductListResponse getStoreProducts(@PathVariable String storeId) {
        return storeService.getStoreProducts(storeId);
    }

    @PatchMapping("/{storeId}/products/{productCode}/availability")
    public ProductDetailResponse updateProductAvailability(@PathVariable String storeId,
            @PathVariable String productCode, @RequestBody UpdateProductAvailabilityRequest request) {
        if (request == null || request.getAvailable() == null) {
            throw new IllegalArgumentException("Availability must not be null");
        }
        return storeService.updateProductAvailability(storeId, productCode, request.getAvailable());
    }
}
