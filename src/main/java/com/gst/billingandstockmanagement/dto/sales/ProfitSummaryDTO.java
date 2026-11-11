package com.gst.billingandstockmanagement.dto.sales;

import java.time.LocalDate;

import lombok.Data;

@Data
public class ProfitSummaryDTO {
    private Double salesExGst;          // all sales in the period, excluding GST
    private Double salesWithCost;       // the part of those sales that has a known cost
    private Double cost;
    private Double profit;              // salesWithCost - cost
    private Double marginPercent;       // profit / salesWithCost
    private Double coveragePercent;     // salesWithCost / salesExGst
    private Long totalLines;
    private Long costedLines;
    private LocalDate trackingSince;    // earliest bill with cost data, over all time
    private Long batchesMissingCost;    // stock on hand with unknown cost
}