package com.gst.billingandstockmanagement.dto;

import lombok.Data;

@Data
public class PurchaserDTO {
    private Long id;
    private Long userId;
    private String name;
    private String dl1;
    private String dl2;
    private String gstin;
}