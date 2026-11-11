package com.gst.billingandstockmanagement.services.stock;

import java.util.List;

import com.gst.billingandstockmanagement.dto.InventoryValueDTO;
import com.gst.billingandstockmanagement.dto.StockDTO;
import com.gst.billingandstockmanagement.entities.Stock;
import com.gst.billingandstockmanagement.entities.User;
import org.springframework.data.domain.Page;
import com.gst.billingandstockmanagement.dto.SearchRequest;

import org.springframework.data.domain.Pageable;

public interface StockService {

    StockDTO getStockById(Long stockId);

    StockDTO addStock(StockDTO stockDTO);
    
    List<Stock> getStockByUser(User user);

	void updateStock(StockDTO stockDTO);

	Page<Stock> searchWithPagination(SearchRequest request);

    void deleteStock(Long stockId);

    void sendExpiryAlerts();

    InventoryValueDTO getTotalInventoryValue(Long userId);

    Page<Stock> getMissingCostStock(Long userId, Pageable pageable);

    void updateStockCost(Long stockId, Long userId, Double costRate);
}