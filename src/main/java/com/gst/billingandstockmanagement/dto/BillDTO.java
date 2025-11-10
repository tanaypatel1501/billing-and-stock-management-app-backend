package com.gst.billingandstockmanagement.dto;

import java.util.Date;
import lombok.Data;

@Data
public class BillDTO {
    private Long id;
    private Long userId;
    private String purchaserName;
    private String dl1;
    private String dl2;
    private String gstin;
    private Date invoiceDate;

    private Double totalAmount;
}

