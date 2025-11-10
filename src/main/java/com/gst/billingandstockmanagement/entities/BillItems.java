package com.gst.billingandstockmanagement.entities;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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
	@JsonManagedReference // Include Product in JSON
    private Product product;
	
	private String batchNo;
	private int quantity;
	private int free;
    private Double rate;
    private Date expiryDate;
    private Double amount;
}
