package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.dto.sales.PeriodSalesDTO;
import com.gst.billingandstockmanagement.dto.sales.SalesSummaryDTO;
import com.gst.billingandstockmanagement.dto.sales.TopProductDTO;
import com.gst.billingandstockmanagement.dto.sales.ProfitTotalsDTO;
import com.gst.billingandstockmanagement.entities.BillItems;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDate;

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
          AND b.reverted = false
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
          AND b.reverted = false
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
          AND b.reverted = false
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
          AND b.reverted = false
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
          AND b.reverted = false
        ORDER BY YEAR(b.invoiceDate) DESC
    """)
    List<Integer> findAvailableYears(@Param("userId") Long userId);

    // --- Profit totals (only lines with a known cost count towards profit) ---
    @Query("""
        SELECT new com.gst.billingandstockmanagement.dto.sales.ProfitTotalsDTO(
            SUM(bi.rate * bi.quantity),
            SUM(CASE WHEN bi.snapshotCost IS NOT NULL THEN bi.rate * bi.quantity ELSE 0.0 END),
            SUM(CASE WHEN bi.snapshotCost IS NOT NULL THEN bi.snapshotCost * (bi.quantity + bi.free) ELSE 0.0 END),
            COUNT(bi),
            SUM(CASE WHEN bi.snapshotCost IS NOT NULL THEN 1L ELSE 0L END)
        )
        FROM BillItems bi
        JOIN bi.bill b
        WHERE b.user.id = :userId
          AND b.reverted = false
          AND (:paidOnly = false OR b.paid = true)
          AND (:fromDate IS NULL OR b.invoiceDate >= :fromDate)
          AND (:toDate IS NULL OR b.invoiceDate <= :toDate)
    """)
    ProfitTotalsDTO findProfitTotals(
            @Param("userId") Long userId,
            @Param("paidOnly") boolean paidOnly,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    // --- First bill that has cost data (profit is only meaningful from here on) ---
    @Query("""
        SELECT MIN(b.invoiceDate)
        FROM BillItems bi
        JOIN bi.bill b
        WHERE b.user.id = :userId
          AND b.reverted = false
          AND bi.snapshotCost IS NOT NULL
    """)
    LocalDate findTrackingSince(@Param("userId") Long userId);
}