package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.entities.StockLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockLogRepository extends JpaRepository<StockLog, Long> {
    List<StockLog> findByStockIdOrderByTimestampDesc(Long stockId);
}