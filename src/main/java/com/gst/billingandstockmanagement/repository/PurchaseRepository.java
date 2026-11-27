package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.entities.Purchase;
import com.gst.billingandstockmanagement.dto.sales.MonthlyInputGstRow;
import com.gst.billingandstockmanagement.dto.sales.OutstandingRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    // Ownership-safe fetch: returns empty if the purchase belongs to someone else
    Optional<Purchase> findByIdAndUserId(Long id, Long userId);

    // Row lock while a payment is written, so two payments cannot both pass the "not above outstanding" check
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Purchase p WHERE p.id = :id")
    Optional<Purchase> findByIdForUpdate(@Param("id") Long id);

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

    // Input GST per month, from the totals stored on each (non-reverted) purchase
    @Query(value = """
        SELECT DATE_FORMAT(p.invoice_date, '%Y-%m') AS ym,
               COALESCE(SUM(p.taxable_amount), 0) AS taxable,
               COALESCE(SUM(p.cgst_amount), 0) AS cgst,
               COALESCE(SUM(p.sgst_amount), 0) AS sgst,
               COALESCE(SUM(p.igst_amount), 0) AS igst
        FROM purchase p
        WHERE p.user_id = :userId
          AND p.reverted = 0
          AND p.invoice_date BETWEEN :fromDate AND :toDate
        GROUP BY DATE_FORMAT(p.invoice_date, '%Y-%m')
        ORDER BY ym
        """, nativeQuery = true)
    List<MonthlyInputGstRow> findMonthlyInputGst(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("SELECT MIN(p.invoiceDate) FROM Purchase p WHERE p.user.id = :userId AND p.reverted = false")
    LocalDate findFirstPurchaseDate(@Param("userId") Long userId);

    // Open purchases: not reverted, with money still owed. Aggregated into the payables report by OutstandingReportBuilder.
    @Query(value = """
        SELECT p.supplier_id AS partyId,
               COALESCE(s.name, p.supplier_name) AS partyName,
               DATE_FORMAT(p.invoice_date, '%Y-%m-%d') AS invoiceDate,
               p.total_amount AS total,
               COALESCE(pay.paid, 0) AS paid
        FROM purchase p
        LEFT JOIN supplier s ON s.id = p.supplier_id
        LEFT JOIN (SELECT purchase_id, SUM(amount) AS paid
                   FROM purchase_payment
                   GROUP BY purchase_id) pay ON pay.purchase_id = p.id
        WHERE p.user_id = :userId
          AND p.reverted = 0
          AND p.total_amount - COALESCE(pay.paid, 0) > 0.005
        ORDER BY p.invoice_date, p.id
        """, nativeQuery = true)
    List<OutstandingRow> findOutstandingPurchases(@Param("userId") Long userId);
}