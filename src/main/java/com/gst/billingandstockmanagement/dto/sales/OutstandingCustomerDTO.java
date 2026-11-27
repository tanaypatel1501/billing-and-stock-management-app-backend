package com.gst.billingandstockmanagement.dto.sales;

import java.time.LocalDate;

import lombok.Data;

@Data
public class OutstandingCustomerDTO {
    private Long partyId;               // customer id (bills) or supplier id (purchases); null when not linked to a saved record
    private String name;
    // number of open invoices (bills or purchases)
    private Long billCount;
    private Double outstanding;

    // outstanding split by age of the invoice, in days
    private Double age0to30;
    private Double age31to60;
    private Double age61to90;
    private Double ageOver90;

    private LocalDate oldestInvoiceDate;
    private Integer oldestAgeDays;
}