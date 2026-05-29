package com.gst.billingandstockmanagement.services.stocklogs;

import com.gst.billingandstockmanagement.dto.StockLogDTO;
import java.util.List;

public interface StockLogService {
    StockLogDTO addLog(StockLogDTO stockLogDTO);
    List<StockLogDTO> getLogsByStockId(Long stockId);
}