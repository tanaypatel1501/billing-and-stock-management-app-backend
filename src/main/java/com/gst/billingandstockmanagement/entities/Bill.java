package com.gst.billingandstockmanagement.entities;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name="bill")
@Data
public class Bill {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(cascade = CascadeType.REMOVE)
	@JoinColumn(name = "user_id")
	@JsonIgnore
	private User user;
	
	private String purchaserName;
	private String dl1;
	private String dl2;
	private String gstin;
	private Date invoiceDate;	
	
	private Double totalAmount;
	
	@OneToMany(mappedBy = "bill", cascade = CascadeType.REMOVE)
	private List<BillItems> billItems;

}

