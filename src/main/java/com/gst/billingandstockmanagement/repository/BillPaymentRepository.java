package com.gst.billingandstockmanagement.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gst.billingandstockmanagement.entities.BillPayment;

public interface BillPaymentRepository extends JpaRepository<BillPayment, Long> {

    List<BillPayment> findByBillIdOrderByPaymentDateAscIdAsc(Long billId);

    // Ownership-safe lookup, same pattern as PurchaseRepository
    Optional<BillPayment> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM BillPayment p WHERE p.bill.id = :billId")
    Double sumByBillId(@Param("billId") Long billId);

    // Batch version for the bills list: rows of [billId, paidSoFar]
    @Query("SELECT p.bill.id, SUM(p.amount) FROM BillPayment p "
            + "WHERE p.bill.id IN :billIds GROUP BY p.bill.id")
    List<Object[]> sumByBillIds(@Param("billIds") Collection<Long> billIds);
}