package com.gst.billingandstockmanagement.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class PurchaseDTO {
    private Long id;

    // Set by the controller from the JWT, never trusted from the client
    private Long userId;

    // Input: the supplier must already exist (saved via /api/supplier/save)
    private Long supplierId;

    // Snapshots, filled by the server from the supplier
    private String supplierName;
    private String supplierGstin;

    // Input fields from the client
    private String supplierInvoiceNo;
    private LocalDate invoiceDate;
    private String taxMode;                 // "CGST_SGST" or "IGST"
    private List<PurchaseItemsDTO> purchaseItems;

    // Computed by the server from the items. Client values are ignored.
    private Double taxableAmount;
    private Double cgstAmount;
    private Double sgstAmount;
    private Double igstAmount;
    private Double totalAmount;

    private boolean reverted;
    private LocalDateTime createdAt;
}