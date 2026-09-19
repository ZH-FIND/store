package com.coffeeflow.backend.dto;

/** 取消订单请求：需同时提供取餐码与手机号后四位（双因子校验）。 */
public class CancelOrderRequest {
    private String pickupCode;
    private String phoneLast4;

    public String getPickupCode() { return pickupCode; }
    public void setPickupCode(String pickupCode) { this.pickupCode = pickupCode; }
    public String getPhoneLast4() { return phoneLast4; }
    public void setPhoneLast4(String phoneLast4) { this.phoneLast4 = phoneLast4; }
}
