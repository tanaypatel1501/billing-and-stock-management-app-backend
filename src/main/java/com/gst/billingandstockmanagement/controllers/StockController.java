package com.gst.billingandstockmanagement.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
import com.gst.billingandstockmanagement.services.stock.StockService;
import com.gst.billingandstockmanagement.services.user.UserService;

import java.util.HashMap;

@RestController
@RequestMapping("/api/stock")
public class StockController {
    @Autowired
    private StockService stockService;
    
    @Autowired
    private UserService userService;

    @GetMapping("/{stockId}")
    public ResponseEntity<StockDTO> getStockById(@PathVariable Long stockId) {
        return ResponseEntity.ok(stockService.getStockById(stockId));
    }

    @DeleteMapping("/{stockId}")
    public ResponseEntity<StockDTO> deleteStockById(@PathVariable Long stockId) {
        stockService.deleteStock(stockId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/add")
    public void addStock(@RequestBody StockDTO stockDTO) {
        stockService.addStock(stockDTO);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<Stock>> getStockByUser(
            @PathVariable Long userId,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size
    ) {
        // Ensure user exists (will throw if not found)
        userService.getUserById(userId);

        SearchRequest req = new SearchRequest();
        req.setPage(page);
        req.setSize(size);
        if (req.getFilters() == null) req.setFilters(new HashMap<>());
        req.getFilters().put("user.id", String.valueOf(userId));
        Page<Stock> p = stockService.searchWithPagination(req);
        return ResponseEntity.ok(p);
    }

    @PutMapping("/update")
    public void updateStock(@RequestBody StockDTO stockDTO) {
        stockService.updateStock(stockDTO);
    }

    @PostMapping("/search")
    public ResponseEntity<Page<Stock>> searchStock(@RequestBody SearchRequest request) {
        Page<Stock> p = stockService.searchWithPagination(request);
        return ResponseEntity.ok(p);
    }
}