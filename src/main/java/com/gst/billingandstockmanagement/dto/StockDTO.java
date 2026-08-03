package com.gst.billingandstockmanagement.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class StockDTO {
	private Long id;
	private Long userId;
	private Long productId;
	private int quantity;
	private String batchNo;
	private LocalDate expiryDate;
	private Double mrp;
}
