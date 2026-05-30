package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.entities.StockLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockLogRepository extends JpaRepository<StockLog, Long> {

    List<StockLog> findByStockIdOrderByTimestampDesc(Long stockId);

    @Query("""
        SELECT sl FROM StockLog sl
        JOIN sl.stock s
        JOIN s.user u
        WHERE u.id = :userId
        AND (:search IS NULL OR :search = ''
             OR LOWER(s.product.name) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(s.batchNo) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY sl.timestamp DESC
    """)
    Page<StockLog> findByUserIdWithSearch(
            @Param("userId") Long userId,
            @Param("search") String search,
            Pageable pageable
    );
}