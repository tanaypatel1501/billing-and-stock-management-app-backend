package com.gst.billingandstockmanagement.dto.sales;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesSummaryDTO {
    private Double totalRevenue;
    private Long totalUnitsSold;
    private Long totalBills;

    public Double getAverageBillValue() {
        if (totalBills == null || totalBills == 0) return 0.0;
        return totalRevenue / totalBills;
    }
}