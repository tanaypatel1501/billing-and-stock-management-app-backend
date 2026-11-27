package com.gst.billingandstockmanagement.dto.sales;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class OutstandingReportDTO {
    private LocalDate asOf;
    private Long openBills;
    private Long customerCount;
    private Double totalOutstanding;

    private Double age0to30;
    private Double age31to60;
    private Double age61to90;
    private Double ageOver90;

    // largest outstanding first
    private List<OutstandingCustomerDTO> customers;
}