package com.gst.billingandstockmanagement.dto;

import lombok.Data;

@Data
public class ProductDTO {
	private Long id;
	private String name;
	private String HSN; 
	private Double MRP;
	private Double CGST;
	private Double SGST;
	private String Packing;
}
