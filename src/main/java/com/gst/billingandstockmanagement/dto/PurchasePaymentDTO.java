package com.gst.billingandstockmanagement.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

// Used as the request body for a payment, as the "initialPayment" of a new purchase,
// and as the response item in payment lists.
@Data
public class PurchasePaymentDTO {
    private Long id;
    private Long purchaseId;
    private Double amount;          // on a new purchase's initialPayment: null means the full total
    private LocalDate paymentDate;
    private String mode;
    private String reference;
    private String notes;
    private LocalDateTime createdAt;
}