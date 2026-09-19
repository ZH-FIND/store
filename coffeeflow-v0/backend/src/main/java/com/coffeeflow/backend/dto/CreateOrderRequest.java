package com.coffeeflow.backend.dto;

import java.util.ArrayList;
import java.util.List;

/** 顾客下单请求：门店 + 顾客信息 + 商品明细。 */
public class CreateOrderRequest {
    private String storeId;
    private String customerName;
    private String phoneLast4;
    private String note;
    private List<Item> items = new ArrayList<>();

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPhoneLast4() { return phoneLast4; }
    public void setPhoneLast4(String phoneLast4) { this.phoneLast4 = phoneLast4; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items == null ? new ArrayList<>() : items; }

    /** 明细项：商品码、规格（不参与计价）、数量。 */
    public static class Item {
        private String productCode;
        private String size;
        private Integer quantity;

        public String getProductCode() { return productCode; }
        public void setProductCode(String productCode) { this.productCode = productCode; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
