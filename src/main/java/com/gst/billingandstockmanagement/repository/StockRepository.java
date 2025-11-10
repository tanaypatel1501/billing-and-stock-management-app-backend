package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.entities.Product;
import com.gst.billingandstockmanagement.entities.Stock;
import com.gst.billingandstockmanagement.entities.User;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

	Stock findByUserAndProduct(User user, Product product);
	
	List<Stock> findByUser(User user);

	Stock findByUserAndProductAndBatchNoAndExpiryDate(User user, Product product, String batchNo, Date expiryDate);
}