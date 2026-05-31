package com.gst.billingandstockmanagement.dto;

import com.gst.billingandstockmanagement.enums.ProductRequestStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProductRequestDTO {

    private Long id;

    // --- Fields the user fills in ---
    private String name;
    private String HSN;
    private Double MRP;
    private Double CGST;
    private Double SGST;
    private String packing;
    private String notes;           // user's reason / extra context

    // --- Read-only fields returned in responses ---
    private ProductRequestStatus status;
    private String adminNotes;      // admin's rejection/approval note
    private Long requestedById;
    private String requestedByName; // "firstname lastname" for admin view
    private String requestedByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}