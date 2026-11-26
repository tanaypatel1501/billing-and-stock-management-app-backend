package com.gst.billingandstockmanagement.dto.sales;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class GstSummaryDTO {
    private int fyStartYear;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDate firstBillDate;         // earliest bill, all time (null if none)
    private LocalDate firstPurchaseDate;     // earliest purchase, all time (null if none)
    private List<GstMonthDTO> months;        // always 12, April first
    private GstMonthDTO totals;              // sales over all 12 months; input and net only over tracked months
    private int trackedMonths;
}