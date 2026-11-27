package com.gst.billingandstockmanagement.dto;

import java.util.List;

import lombok.Data;

// Money position of one bill. All figures are computed by the server.
@Data
public class BillBalanceDTO {
    private Long billId;
    private Double totalAmount;
    private Double paidAmount;
    private Double outstandingAmount;

    // "UNPAID", "PARTIAL" or "PAID"
    private String status;

    // Filled for the single-bill endpoint, left null in the batch endpoint
    private List<BillPaymentDTO> payments;
}