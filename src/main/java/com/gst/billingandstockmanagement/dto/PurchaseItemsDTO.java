package com.gst.billingandstockmanagement.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class PurchaseItemsDTO {
    private Long id;
    private Long purchaseId;
    private Long productId;
    private Long stockId;

    // Filled by the server from the product master (ignored if sent by the client)
    private String snapshotProductName;
    private String snapshotPacking;
    private String snapshotHsn;
    private Double snapshotCgst;
    private Double snapshotSgst;

    // Input fields from the client
    private String batchNo;
    private LocalDate expiryDate;
    private Double mrp;
    private int quantity;
    private int freeQuantity;
    private Double purchaseRate;
    private Double discountPercent;

    // Computed by the server: quantity x purchaseRate x (1 - discount%). Client value is ignored.
    private Double amount;
}