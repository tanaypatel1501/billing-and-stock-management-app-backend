package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.entities.Bill;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.dto.sales.OutstandingBillRow;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BillRepository extends JpaRepository<Bill, Long>, JpaSpecificationExecutor<Bill> {

    // Custom query to calculate the total amount of a bill based on its bill items
    @Query("SELECT SUM(bi.amount) FROM BillItems bi WHERE bi.bill = ?1")
    Double calculateTotalAmount(Bill bill);

	List<Bill> findByUser(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bill b WHERE b.id = :id")
    Optional<Bill> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Bill b SET b.purchaser = null WHERE b.purchaser.id = :purchaserId")
    void nullifyPurchaserReference(@Param("purchaserId") Long purchaserId);

    @Query("SELECT MIN(b.invoiceDate) FROM Bill b WHERE b.user.id = :userId AND b.reverted = false")
    LocalDate findFirstBillDate(@Param("userId") Long userId);

    // Open bills: not reverted, with money still outstanding. Aggregated into the aging report by the service.
    @Query(value = """
        SELECT b.id AS billId,
               b.purchaser_id AS purchaserId,
               COALESCE(pu.name, b.purchaser_name) AS customerName,
               DATE_FORMAT(b.invoice_date, '%Y-%m-%d') AS invoiceDate,
               b.total_amount AS total,
               COALESCE(pay.paid, 0) AS paid
        FROM bill b
        LEFT JOIN purchaser pu ON pu.id = b.purchaser_id
        LEFT JOIN (SELECT bill_id, SUM(amount) AS paid
                   FROM bill_payment
                   GROUP BY bill_id) pay ON pay.bill_id = b.id
        WHERE b.user_id = :userId
          AND b.reverted = 0
          AND b.total_amount - COALESCE(pay.paid, 0) > 0.005
        ORDER BY b.invoice_date, b.id
        """, nativeQuery = true)
    List<OutstandingBillRow> findOutstandingBills(@Param("userId") Long userId);
}
