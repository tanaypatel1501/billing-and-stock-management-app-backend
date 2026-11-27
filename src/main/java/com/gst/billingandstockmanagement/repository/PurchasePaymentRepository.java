package com.gst.billingandstockmanagement.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gst.billingandstockmanagement.entities.PurchasePayment;

public interface PurchasePaymentRepository extends JpaRepository<PurchasePayment, Long> {

    List<PurchasePayment> findByPurchaseIdOrderByPaymentDateAscIdAsc(Long purchaseId);

    // Ownership-safe lookup
    Optional<PurchasePayment> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PurchasePayment p WHERE p.purchase.id = :purchaseId")
    Double sumByPurchaseId(@Param("purchaseId") Long purchaseId);

    // Batch version for the purchases list: rows of [purchaseId, paidSoFar]
    @Query("SELECT p.purchase.id, SUM(p.amount) FROM PurchasePayment p "
            + "WHERE p.purchase.id IN :purchaseIds GROUP BY p.purchase.id")
    List<Object[]> sumByPurchaseIds(@Param("purchaseIds") Collection<Long> purchaseIds);
}