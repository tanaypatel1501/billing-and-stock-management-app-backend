package com.gst.billingandstockmanagement.dto.sales;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopProductDTO {
    private String productName;
    private Long totalQuantitySold;
    private Double totalRevenue;
}