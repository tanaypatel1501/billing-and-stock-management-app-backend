package com.gst.billingandstockmanagement.dto;

public class InventoryValueDTO {
    private Double activeValue;
    private Double expiredValue;

    public InventoryValueDTO() {}

    public InventoryValueDTO(Double activeValue, Double expiredValue) {
        this.activeValue = activeValue;
        this.expiredValue = expiredValue;
    }

    public Double getActiveValue() { return activeValue; }
    public void setActiveValue(Double activeValue) { this.activeValue = activeValue; }

    public Double getExpiredValue() { return expiredValue; }
    public void setExpiredValue(Double expiredValue) { this.expiredValue = expiredValue; }
}