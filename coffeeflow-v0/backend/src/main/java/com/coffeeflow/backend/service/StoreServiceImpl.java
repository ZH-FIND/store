package com.coffeeflow.backend.service;

import com.coffeeflow.backend.dto.ProductDetailResponse;
import com.coffeeflow.backend.dto.ProductListResponse;
import com.coffeeflow.backend.dto.StoreDetailResponse;
import com.coffeeflow.backend.dto.StoreListResponse;
import com.coffeeflow.backend.model.Product;
import com.coffeeflow.backend.model.Store;
import com.coffeeflow.backend.repository.ProductRepository;
import com.coffeeflow.backend.repository.StoreRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreServiceImpl implements StoreService {
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;

    public StoreServiceImpl(StoreRepository storeRepository, ProductRepository productRepository) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public StoreListResponse getStores() {
        List<StoreDetailResponse> stores = storeRepository.findAll().stream()
                .map(store -> new StoreDetailResponse(store.getStoreId(), store.getStoreName()))
                .collect(Collectors.toList());
        return new StoreListResponse(stores.size(), stores);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductListResponse getStoreProducts(String storeId) {
        Store store = findStore(storeId);
        List<ProductDetailResponse> products = productRepository.findByStoreId(store.getStoreId()).stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());
        return new ProductListResponse(products.size(), products);
    }

    @Override
    @Transactional
    public ProductDetailResponse updateProductAvailability(String storeId, String productCode,
            boolean available) {
        Store store = findStore(storeId);
        findStoreProduct(store.getStoreId(), productCode);
        productRepository.updateAvailability(store.getStoreId(), productCode, available);
        return toProductResponse(findStoreProduct(store.getStoreId(), productCode));
    }

    private Store findStore(String storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new NoSuchElementException("Store not found: " + storeId));
    }

    private Product findStoreProduct(String storeId, String productCode) {
        return productRepository.findStoreProduct(storeId, productCode)
                .orElseThrow(() -> new NoSuchElementException(
                        "Product not found in store " + storeId + ": " + productCode));
    }

    private ProductDetailResponse toProductResponse(Product product) {
        return new ProductDetailResponse(product.getProductCode(), product.getName(),
                product.getPrice(), product.getAvailable());
    }
}
