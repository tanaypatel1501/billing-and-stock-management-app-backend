package com.gst.billingandstockmanagement.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class BillItemsDTO {
    private Long id;
    private Long billId;
    private Long productId;
    private Long stockId;
    private String snapshotProductName;
    private Double snapshotUnitPrice;
    private String snapshotPacking;
    private String snapshotHsn;
    private Double snapshotCgst;
    private Double snapshotSgst;
    private String batchNo;
    private int quantity;
    private int free;
    private Double rate;
    private LocalDate expiryDate;
    private Double amount;
}
