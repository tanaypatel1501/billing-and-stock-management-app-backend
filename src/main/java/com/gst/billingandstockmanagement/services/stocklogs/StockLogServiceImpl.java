package com.gst.billingandstockmanagement.services.stocklogs;

import com.gst.billingandstockmanagement.dto.StockLogDTO;
import com.gst.billingandstockmanagement.entities.Stock;
import com.gst.billingandstockmanagement.entities.StockLog;
import com.gst.billingandstockmanagement.repository.StockLogRepository;
import com.gst.billingandstockmanagement.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    @Override
    @Transactional(readOnly = true)
    public Page<StockLogDTO> getLogsByUserId(Long userId, String search, Pageable pageable) {
        Page<StockLog> logs = stockLogRepository.findByUserIdWithSearch(userId, search, pageable);
        return logs.map(this::mapToDTO);
    }

    // Update mapToDTO to include enriched fields
    private StockLogDTO mapToDTO(StockLog log) {
        StockLogDTO dto = new StockLogDTO();
        dto.setId(log.getId());
        dto.setAction(log.getAction());
        dto.setNotes(log.getNotes());
        dto.setTimestamp(log.getTimestamp());

        if (log.getStock() != null) {
            dto.setStockId(log.getStock().getId());
            dto.setBatchNo(log.getStock().getBatchNo());

            if (log.getStock().getProduct() != null) {
                dto.setProductName(log.getStock().getProduct().getName());
                dto.setProductPacking(log.getStock().getProduct().getPacking());
            }
        }
        return dto;
    }
}