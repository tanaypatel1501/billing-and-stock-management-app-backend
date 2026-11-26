package com.gst.billingandstockmanagement.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BillPaymentDTO {
    private Long id;
    private Long billId;
    private Double amount;
    private LocalDate paymentDate;
    private String mode;
    private String reference;
    private String notes;
    private LocalDateTime createdAt;
}