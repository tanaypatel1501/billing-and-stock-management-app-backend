package com.gst.billingandstockmanagement.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"user", "supplier", "purchaseItems"})
@Entity
@Table(name = "purchase")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = true)
    private Supplier supplier;

    // Snapshots, so old purchases survive supplier edits or deletion
    private String supplierName;
    private String supplierGstin;
    private String supplierInvoiceNo;

    private LocalDate invoiceDate;

    // "CGST_SGST" or "IGST"
    private String taxMode = "CGST_SGST";

    private Double taxableAmount = 0.0;
    private Double cgstAmount = 0.0;
    private Double sgstAmount = 0.0;
    private Double igstAmount = 0.0;
    private Double totalAmount = 0.0;

    @Column(nullable = false)
    private boolean reverted = false;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.REMOVE)
    @JsonIgnore
    private List<PurchaseItems> purchaseItems;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        }
    }
}