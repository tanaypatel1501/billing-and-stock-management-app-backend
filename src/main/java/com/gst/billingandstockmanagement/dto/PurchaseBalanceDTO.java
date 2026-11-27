package com.gst.billingandstockmanagement.dto;

import java.util.List;

import lombok.Data;

// Money position of one purchase. All figures are computed by the server.
@Data
public class PurchaseBalanceDTO {
    private Long purchaseId;
    private Double totalAmount;
    private Double paidAmount;
    private Double outstandingAmount;

    // "UNPAID", "PARTIAL" or "PAID"
    private String status;

    private List<PurchasePaymentDTO> payments;
}