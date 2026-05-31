package com.gst.billingandstockmanagement.entities;

import com.gst.billingandstockmanagement.enums.ProductRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_request")
@Getter
@Setter
public class ProductRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String HSN;
    private Double MRP;
    private Double CGST;
    private Double SGST;
    private String packing;

    // Optional note from the user explaining why they need this product
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductRequestStatus status = ProductRequestStatus.PENDING;

    // Admin's reason when rejecting (or a note when approving)
    private String adminNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;
}