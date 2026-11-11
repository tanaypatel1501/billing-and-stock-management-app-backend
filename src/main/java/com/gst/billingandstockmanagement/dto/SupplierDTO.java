package com.gst.billingandstockmanagement.dto;

import lombok.Data;

@Data
public class SupplierDTO {
    private Long id;
    private Long userId;
    private String name;
    private String dl1;
    private String dl2;
    private String gstin;
    private String state;
    private String phoneNumber;
}