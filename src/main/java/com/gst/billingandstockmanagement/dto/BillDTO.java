package com.gst.billingandstockmanagement.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class BillDTO {
    private Long id;
    private Long userId;
    private String purchaserName;
    private String dl1;
    private String dl2;
    private String gstin;
    private LocalDate invoiceDate;

    private Double totalAmount;
    private boolean paid;

    private List<BillItemsDTO> billItems;
    private Long purchaserId;
    private boolean reverted;
}
