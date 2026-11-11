package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.entities.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    // Ownership-safe fetch: returns empty if the purchase belongs to someone else
    Optional<Purchase> findByIdAndUserId(Long id, Long userId);

    // Paginated list for the purchases screen. Reverted purchases are hidden.
    // Search matches supplier name or supplier invoice number; dates are optional.
    @Query("""
        SELECT p FROM Purchase p
        WHERE p.user.id = :userId
          AND p.reverted = false
          AND (:search IS NULL OR :search = ''
               OR LOWER(p.supplierName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.supplierInvoiceNo) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:fromDate IS NULL OR p.invoiceDate >= :fromDate)
          AND (:toDate IS NULL OR p.invoiceDate <= :toDate)
        ORDER BY p.invoiceDate DESC, p.id DESC
    """)
    Page<Purchase> search(
            @Param("userId") Long userId,
            @Param("search") String search,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    // Duplicate guard: same supplier + same invoice number, ignoring reverted purchases
    @Query("""
        SELECT COUNT(p) > 0 FROM Purchase p
        WHERE p.user.id = :userId
          AND p.reverted = false
          AND LOWER(p.supplierName) = LOWER(:supplierName)
          AND LOWER(p.supplierInvoiceNo) = LOWER(:invoiceNo)
    """)
    boolean existsActiveDuplicate(
            @Param("userId") Long userId,
            @Param("supplierName") String supplierName,
            @Param("invoiceNo") String invoiceNo
    );

    // Used when a supplier is deleted, same approach as BillRepository.nullifyPurchaserReference
    @Modifying
    @Transactional
    @Query("UPDATE Purchase p SET p.supplier = null WHERE p.supplier.id = :supplierId")
    void nullifySupplierReference(@Param("supplierId") Long supplierId);
}