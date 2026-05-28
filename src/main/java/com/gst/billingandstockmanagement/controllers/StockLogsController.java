package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.dto.StockLogDTO;

import com.gst.billingandstockmanagement.services.stocklogs.StockLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-logs")
public class StockLogsController {

    @Autowired
    private StockLogService stockLogService;

    @PostMapping
    public ResponseEntity<StockLogDTO> createLog(@RequestBody StockLogDTO stockLogDTO) {
        StockLogDTO savedLog = stockLogService.addLog(stockLogDTO);
        return new ResponseEntity<>(savedLog, HttpStatus.CREATED);
    }

    @GetMapping("/{stockId}")
    public ResponseEntity<List<StockLogDTO>> getStockHistory(@PathVariable Long stockId) {
        List<StockLogDTO> logs = stockLogService.getLogsByStockId(stockId);
        return ResponseEntity.ok(logs);
    }
}