package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.entities.Product;
import com.gst.billingandstockmanagement.entities.Stock;
import com.gst.billingandstockmanagement.entities.User;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockRepository extends JpaRepository<Stock, Long>, JpaSpecificationExecutor<Stock> {

	List<Stock> findByUser(User user);

	Optional<Stock> findByUserAndProductAndBatchNoAndExpiryDateAndMrp(
			User user, Product product, String batchNo, Date expiryDate, Double mrp
	);

	List<Stock> findByExpiryDateBetweenAndLastExpiryNotificationDateIsNull(Date start, Date end);

	List<Stock> findByExpiryDateBeforeAndExpiredNotificationDateIsNull(Date date);
}