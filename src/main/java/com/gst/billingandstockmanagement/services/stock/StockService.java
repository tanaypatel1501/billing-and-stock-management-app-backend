package com.gst.billingandstockmanagement.services.stock;

import java.util.List;

import com.gst.billingandstockmanagement.dto.StockDTO;
import com.gst.billingandstockmanagement.entities.Stock;
import com.gst.billingandstockmanagement.entities.User;
import org.springframework.data.domain.Page;
import com.gst.billingandstockmanagement.dto.SearchRequest;

public interface StockService {

    StockDTO getStockById(Long stockId);

    void addStock(StockDTO stockDTO);
    
    List<Stock> getStockByUser(User user);

	void updateStock(StockDTO stockDTO);

	Page<Stock> searchWithPagination(SearchRequest request);

    void deleteStock(Long stockId);
}