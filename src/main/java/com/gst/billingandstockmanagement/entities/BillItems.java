package com.gst.billingandstockmanagement.entities;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"bill", "product"})
@Entity
@Table(name = "bill_items")
public class BillItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bill_id")
    @JsonBackReference
    private Bill bill;

    @ManyToOne
    @JoinColumn(name = "product_id")
    @JsonIgnore
    private Product product;

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
    private Date expiryDate;
    private Double amount;
}
