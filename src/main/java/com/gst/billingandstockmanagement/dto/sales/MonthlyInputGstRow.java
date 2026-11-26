package com.gst.billingandstockmanagement.dto.sales;

public interface MonthlyInputGstRow {
    String getYm();
    Number getTaxable();
    Number getCgst();
    Number getSgst();
    Number getIgst();
}