package com.gst.billingandstockmanagement.dto;

import java.util.Date;

import lombok.Data;

@Data
public class BillItemsDTO {
    private Long id;
    private Long billId;
    private Long productId;
    private String batchNo;
    private int quantity;
    private int free;
    private Double rate;
    private Date expiryDate;
    private Double amount;
}
