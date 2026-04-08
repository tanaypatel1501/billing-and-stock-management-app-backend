package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.dto.sales.PeriodSalesDTO;
import com.gst.billingandstockmanagement.dto.sales.SalesSummaryDTO;
import com.gst.billingandstockmanagement.dto.sales.TopProductDTO;
import com.gst.billingandstockmanagement.entities.BillItems;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BillItemsRepository extends JpaRepository<BillItems, Long> {

    // --- Top selling products ---
    @Query("""
        SELECT new com.gst.billingandstockmanagement.dto.sales.TopProductDTO(
            bi.snapshotProductName,
            SUM(bi.quantity),
            SUM(bi.amount)
        )
        FROM BillItems bi
        JOIN bi.bill b
        WHERE b.user.id = :userId
          AND (:paidOnly = false OR b.paid = true)
        GROUP BY bi.snapshotProductName
        ORDER BY SUM(bi.quantity) DESC
        LIMIT :limit
    """)
    List<TopProductDTO> findTopProducts(
            @Param("userId") Long userId,
            @Param("paidOnly") boolean paidOnly,
            @Param("limit") int limit
    );

    // --- Monthly sales for a given year ---
    @Query("""
        SELECT new com.gst.billingandstockmanagement.dto.sales.PeriodSalesDTO(
            MONTH(b.invoiceDate),
            YEAR(b.invoiceDate),
            SUM(bi.amount),
            SUM(bi.quantity),
            COUNT(DISTINCT b.id)
        )
        FROM BillItems bi
        JOIN bi.bill b
        WHERE b.user.id = :userId
          AND YEAR(b.invoiceDate) = :year
          AND (:paidOnly = false OR b.paid = true)
        GROUP BY YEAR(b.invoiceDate), MONTH(b.invoiceDate)
        ORDER BY MONTH(b.invoiceDate)
    """)
    List<PeriodSalesDTO> findMonthlySales(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("paidOnly") boolean paidOnly
    );

    // --- Yearly sales (all years) ---
    @Query("""
        SELECT new com.gst.billingandstockmanagement.dto.sales.PeriodSalesDTO(
            0,
            YEAR(b.invoiceDate),
            SUM(bi.amount),
            SUM(bi.quantity),
            COUNT(DISTINCT b.id)
        )
        FROM BillItems bi
        JOIN bi.bill b
        WHERE b.user.id = :userId
          AND (:paidOnly = false OR b.paid = true)
        GROUP BY YEAR(b.invoiceDate)
        ORDER BY YEAR(b.invoiceDate)
    """)
    List<PeriodSalesDTO> findYearlySales(
            @Param("userId") Long userId,
            @Param("paidOnly") boolean paidOnly
    );

    // --- Summary card (single row of totals) ---
    @Query("""
        SELECT new com.gst.billingandstockmanagement.dto.sales.SalesSummaryDTO(
            SUM(bi.amount),
            SUM(bi.quantity),
            COUNT(DISTINCT b.id)
        )
        FROM BillItems bi
        JOIN bi.bill b
        WHERE b.user.id = :userId
          AND (:paidOnly = false OR b.paid = true)
    """)
    SalesSummaryDTO findSalesSummary(
            @Param("userId") Long userId,
            @Param("paidOnly") boolean paidOnly
    );

    // --- Available years (to populate year dropdown on frontend) ---
    @Query("""
        SELECT DISTINCT YEAR(b.invoiceDate)
        FROM BillItems bi
        JOIN bi.bill b
        WHERE b.user.id = :userId
        ORDER BY YEAR(b.invoiceDate) DESC
    """)
    List<Integer> findAvailableYears(@Param("userId") Long userId);
}