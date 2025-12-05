package com.gst.billingandstockmanagement.entities;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name="bill_items")
@Data
public class BillItems {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "bill_id")
	@JsonBackReference // Prevent circular reference
    private Bill bill;
	
	@ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "product_id")
	@JsonIgnore // Do not serialize the live Product (use snapshots instead)
    private Product product;
	
	// Snapshot fields: copy current product details at the time of billing
	@Column(name = "snapshot_product_name")
	private String snapshotProductName;
	@Column(name = "snapshot_unit_price")
	private Double snapshotUnitPrice;
    @Column(name= "snapshot_packing")
    private String snapshotPacking;
    @Column(name = "snapshot_hsn")
    private String snapshotHsn;
    @Column(name = "snapshot_cgst")
    private Double snapshotCgst;
    @Column(name = "snapshot_sgst")
    private Double snapshotSgst;

	private String batchNo;
	private int quantity;
	private int free;
    private Double rate;
    private Date expiryDate;
    private Double amount;
}
