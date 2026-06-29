package com.gst.billingandstockmanagement.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;

import com.gst.billingandstockmanagement.dto.StockDTO;
import com.gst.billingandstockmanagement.dto.SearchRequest;
import com.gst.billingandstockmanagement.entities.Stock;
import com.gst.billingandstockmanagement.security.SecurityUtils;
import com.gst.billingandstockmanagement.services.stock.StockService;

import java.util.HashMap;

@RestController
@RequestMapping("/api/stock")
public class StockController {
    @Autowired
    private StockService stockService;

    @GetMapping("/{stockId}")
    public ResponseEntity<StockDTO> getStockById(@PathVariable Long stockId) {
        StockDTO dto = stockService.getStockById(stockId);
        requireOwnership(dto.getUserId());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{stockId}")
    public ResponseEntity<StockDTO> deleteStockById(@PathVariable Long stockId) {
        StockDTO dto = stockService.getStockById(stockId); // throws if not found
        requireOwnership(dto.getUserId());
        stockService.deleteStock(stockId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/add")
    public ResponseEntity<StockDTO> addStock(@RequestBody StockDTO stockDTO) {
        stockDTO.setUserId(SecurityUtils.getCurrentUserId());
        StockDTO savedStockDTO = stockService.addStock(stockDTO);
        return ResponseEntity.ok(savedStockDTO);
    }

    @GetMapping("/user")
    public ResponseEntity<Page<Stock>> getStockByUser(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        SearchRequest req = new SearchRequest();
        req.setPage(page);
        req.setSize(size);
        req.setFilters(new HashMap<>());
        req.getFilters().put("user.id", String.valueOf(currentUserId));
        Page<Stock> p = stockService.searchWithPagination(req);
        return ResponseEntity.ok(p);
    }

    @PutMapping("/update")
    public void updateStock(@RequestBody StockDTO stockDTO) {
        stockDTO.setUserId(SecurityUtils.getCurrentUserId());
        stockService.updateStock(stockDTO);
    }

    @PostMapping("/search")
    public ResponseEntity<Page<Stock>> searchStock(@RequestBody SearchRequest request) {
        if (request.getFilters() == null) request.setFilters(new HashMap<>());
        request.getFilters().put("user.id", String.valueOf(SecurityUtils.getCurrentUserId()));
        Page<Stock> p = stockService.searchWithPagination(request);
        return ResponseEntity.ok(p);
    }

    @GetMapping("/user/inventory-value")
    public ResponseEntity<Double> getInventoryValue() {
        return ResponseEntity.ok(stockService.getTotalInventoryValue(SecurityUtils.getCurrentUserId()));
    }

    private void requireOwnership(Long resourceOwnerId) {
        if (!SecurityUtils.getCurrentUserId().equals(resourceOwnerId)) {
            throw new AccessDeniedException("You do not have access to this resource.");
        }
    }
}