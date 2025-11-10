package com.gst.billingandstockmanagement.services.stock;

import java.util.List;

import com.gst.billingandstockmanagement.dto.StockDTO;
import com.gst.billingandstockmanagement.entities.Stock;
import com.gst.billingandstockmanagement.entities.User;

public interface StockService {
    void addStock(StockDTO stockDTO);
    
    List<Stock> getStockByUser(User user);

	void updateStock(StockDTO stockDTO);
}