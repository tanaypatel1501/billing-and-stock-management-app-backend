package com.gst.billingandstockmanagement.entities;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"user", "billItems"})
@Entity
@Table(name = "bill")
public class Bill {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@ManyToOne
	@JoinColumn(name = "user_id")
	@JsonIgnore
	private User user;

	private String purchaserName;
	private String dl1;
	private String dl2;
	private String gstin;
	private Date invoiceDate;

	private Double totalAmount;

	@Column(nullable = false, columnDefinition = "boolean default false")
	private boolean paid = false;

	@OneToMany(mappedBy = "bill", cascade = CascadeType.REMOVE)
	private List<BillItems> billItems;
}

