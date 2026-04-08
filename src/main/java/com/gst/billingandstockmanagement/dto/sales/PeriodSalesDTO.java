package com.gst.billingandstockmanagement.dto.sales;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PeriodSalesDTO {
    private int month;
    private int year;
    private Double totalRevenue;
    private Long totalUnitsSold;
    private Long totalBills;
}
