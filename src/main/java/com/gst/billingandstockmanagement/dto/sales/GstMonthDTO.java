package com.gst.billingandstockmanagement.dto.sales;

import lombok.Data;

@Data
public class GstMonthDTO {
    private String month;
    private Double salesTaxable = 0.0;  // sales value excluding GST
    private Double salesGst = 0.0;      // GST collected on sales (CGST + SGST/IGST together)
    private Double inputCgst;
    private Double inputSgst;
    private Double inputIgst;
    private Double inputGst;            // total input GST
    private Double netGst;              // salesGst - inputGst (negative means credit)
    private boolean inputTracked;
}