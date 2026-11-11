package com.gst.billingandstockmanagement.dto.sales;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfitTotalsDTO {
    private Double totalRevenue;     // all lines, excluding GST
    private Double costedRevenue;    // lines with a known cost
    private Double costedCost;       // cost of those lines
    private Long totalLines;
    private Long costedLines;
}