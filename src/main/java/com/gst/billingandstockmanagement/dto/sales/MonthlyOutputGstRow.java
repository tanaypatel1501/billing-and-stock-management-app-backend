package com.gst.billingandstockmanagement.dto.sales;

public interface MonthlyOutputGstRow {
    String getYm();
    Number getTaxable();
    Number getGst();
}