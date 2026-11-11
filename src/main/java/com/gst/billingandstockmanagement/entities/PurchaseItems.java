package com.gst.billingandstockmanagement.entities;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"purchase", "product", "stock"})
@Entity
@Table(name = "purchase_items")
public class PurchaseItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "purchase_id")
    @JsonIgnore
    private Purchase purchase;

    @ManyToOne
    @JoinColumn(name = "product_id")
    @JsonIgnore
    private Product product;

    // The stock batch this line created or topped up. Used to revert the purchase.
    @ManyToOne
    @JoinColumn(name = "stock_id")
    @JsonIgnore
    private Stock stock;

    // Snapshots, same idea as BillItems
    private String snapshotProductName;
    private String snapshotPacking;
    private String snapshotHsn;
    private Double snapshotCgst;
    private Double snapshotSgst;

    private String batchNo;
    private LocalDate expiryDate;
    private Double mrp;

    private int quantity;
    private int freeQuantity;

    private Double purchaseRate;
    private Double discountPercent = 0.0;

    // Taxable line amount: quantity x rate, less discount. Free units are not charged.
    private Double amount;
}