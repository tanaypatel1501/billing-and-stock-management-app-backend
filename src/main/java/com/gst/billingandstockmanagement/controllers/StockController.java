package com.gst.billingandstockmanagement.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gst.billingandstockmanagement.dto.StockDTO;
import com.gst.billingandstockmanagement.entities.Stock;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.services.stock.StockService;
import com.gst.billingandstockmanagement.services.user.UserService;

@RestController
@RequestMapping("/api/stock")
public class StockController {
    @Autowired
    private StockService stockService;
    
    @Autowired
    private UserService userService; 

    @PostMapping("/add")
    public void addStock(@RequestBody StockDTO stockDTO) {
        stockService.addStock(stockDTO);
    }
    
    @GetMapping("/user/{userId}")
    public List<Stock> getStockByUser(@PathVariable Long userId) {
    	User user = userService.getUserById(userId);
        return stockService.getStockByUser(user);
    }

    @PostMapping("/update")
    public void updateStock(@RequestBody StockDTO stockDTO) {
        stockService.updateStock(stockDTO);
    }
}