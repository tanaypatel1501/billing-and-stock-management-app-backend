package com.gst.billingandstockmanagement.services.stocklogs;

import com.gst.billingandstockmanagement.dto.StockLogDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface StockLogService {
    StockLogDTO addLog(StockLogDTO stockLogDTO);
    List<StockLogDTO> getLogsByStockId(Long stockId);
    Page<StockLogDTO> getLogsByUserId(Long userId, String search, Pageable pageable);
}