package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.entities.Product;
import com.gst.billingandstockmanagement.entities.Stock;
import com.gst.billingandstockmanagement.entities.User;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface StockRepository extends JpaRepository<Stock, Long>, JpaSpecificationExecutor<Stock> {

	List<Stock> findByUser(User user);

	Stock findByUserAndProductAndBatchNoAndExpiryDate(
			User user, Product product, String batchNo, Date expiryDate
	);

	Optional<Stock> findByUserAndProductAndBatchNoAndExpiryDateAndMrp(
			User user, Product product, String batchNo, Date expiryDate, Double mrp
	);

	List<Stock> findByExpiryDateBetweenAndLastExpiryNotificationDateIsNull(Date start, Date end);

	List<Stock> findByExpiryDateBeforeAndExpiredNotificationDateIsNull(Date date);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Stock s where s.id = :id")
	Optional<Stock> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

	@Query("""
		select coalesce(sum(coalesce(s.mrp, p.MRP, 0.0) * s.quantity), 0.0)
		from Stock s
		join s.product p
		where s.user.id = :userId
	""")
	Double calculateInventoryValueForUser(@Param("userId") Long userId);
}