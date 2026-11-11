package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.entities.Product;
import com.gst.billingandstockmanagement.entities.Stock;
import com.gst.billingandstockmanagement.entities.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;

public interface StockRepository extends JpaRepository<Stock, Long>, JpaSpecificationExecutor<Stock> {

	List<Stock> findByUser(User user);

	Stock findByUserAndProductAndBatchNoAndExpiryDate(
			User user, Product product, String batchNo, LocalDate expiryDate
	);

	Optional<Stock> findByUserAndProductAndBatchNoAndExpiryDateAndMrp(
			User user, Product product, String batchNo, LocalDate expiryDate, Double mrp
	);

	List<Stock> findByExpiryDateBetweenAndLastExpiryNotificationDateIsNull(LocalDate start, LocalDate end);

	List<Stock> findByExpiryDateBeforeAndExpiredNotificationDateIsNull(LocalDate date);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Stock s where s.id = :id")
	Optional<Stock> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

	@Query("""
	   select coalesce(sum(coalesce(s.mrp, p.MRP, 0.0) * s.quantity), 0.0)
	   from Stock s
	   join s.product p
	   where s.user.id = :userId
	   and s.expiryDate >= :today
	""")
	Double calculateActiveInventoryValueForUser(@Param("userId") Long userId, @Param("today") LocalDate today);

	@Query("""
	   select coalesce(sum(coalesce(s.mrp, p.MRP, 0.0) * s.quantity), 0.0)
	   from Stock s
	   join s.product p
	   where s.user.id = :userId
	   and s.expiryDate < :today
	""")
	Double calculateExpiredInventoryValueForUser(@Param("userId") Long userId, @Param("today") LocalDate today);

	// Stock on hand (not expired) whose cost is still unknown
	@Query("""
	   select count(s) from Stock s
	   where s.user.id = :userId and s.quantity > 0 and s.costRate is null
	   and s.expiryDate >= :today
	""")
	long countMissingCost(@Param("userId") Long userId, @Param("today") LocalDate today);

	@Query(value = """
	   select s from Stock s
	   where s.user.id = :userId and s.quantity > 0 and s.costRate is null
	   and s.expiryDate >= :today
	   order by (s.quantity * coalesce(s.mrp, 0.0)) desc, s.id asc
	""", countQuery = """
	   select count(s) from Stock s
	   where s.user.id = :userId and s.quantity > 0 and s.costRate is null
	   and s.expiryDate >= :today
	""")
	Page<Stock> findMissingCost(@Param("userId") Long userId, @Param("today") LocalDate today, Pageable pageable);
}