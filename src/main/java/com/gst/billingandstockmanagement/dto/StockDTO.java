package com.gst.billingandstockmanagement.dto;

import java.util.Date;

import lombok.Data;

@Data
public class StockDTO {
	private Long id;
	private Long userId;
	private Long productId;
	private int quantity;
	private String batchNo;
	private Date expiryDate;
}
