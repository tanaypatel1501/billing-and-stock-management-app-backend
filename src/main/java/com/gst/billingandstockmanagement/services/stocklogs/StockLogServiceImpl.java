package com.gst.billingandstockmanagement.services.stocklogs;

import com.gst.billingandstockmanagement.dto.StockLogDTO;
import com.gst.billingandstockmanagement.entities.Stock;
import com.gst.billingandstockmanagement.entities.StockLog;
import com.gst.billingandstockmanagement.repository.StockLogRepository;
import com.gst.billingandstockmanagement.repository.StockRepository;
import com.gst.billingandstockmanagement.services.stocklogs.StockLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockLogServiceImpl implements StockLogService {

    @Autowired
    private StockLogRepository stockLogRepository;

    @Autowired
    private StockRepository stockRepository;

    @Override
    @Transactional
    public StockLogDTO addLog(StockLogDTO stockLogDTO) {
        StockLog log = new StockLog();

        // Find the stock entry this log belongs to
        Stock stock = stockRepository.findById(stockLogDTO.getStockId())
                .orElseThrow(() -> new RuntimeException("Stock item not found with ID: " + stockLogDTO.getStockId()));

        log.setStock(stock);
        log.setAction(stockLogDTO.getAction().toUpperCase());
        log.setNotes(stockLogDTO.getNotes());

        // Set timestamp to now if not provided explicitly by the request
        if (stockLogDTO.getTimestamp() == null) {
            log.setTimestamp(LocalDateTime.now());
        } else {
            log.setTimestamp(stockLogDTO.getTimestamp());
        }

        StockLog savedLog = stockLogRepository.save(log);
        return mapToDTO(savedLog);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockLogDTO> getLogsByStockId(Long stockId) {
        List<StockLog> logs = stockLogRepository.findByStockIdOrderByTimestampDesc(stockId);
        return logs.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // Helper mapping method
    private StockLogDTO mapToDTO(StockLog log) {
        StockLogDTO dto = new StockLogDTO();
        dto.setId(log.getId());
        if (log.getStock() != null) {
            dto.setStockId(log.getStock().getId());
        }
        dto.setAction(log.getAction());
        dto.setNotes(log.getNotes());
        dto.setTimestamp(log.getTimestamp());
        return dto;
    }
}