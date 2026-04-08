package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.dto.sales.PeriodSalesDTO;
import com.gst.billingandstockmanagement.dto.sales.SalesSummaryDTO;
import com.gst.billingandstockmanagement.dto.sales.TopProductDTO;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.services.sales.SalesService;
import com.gst.billingandstockmanagement.services.user.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SalesController {

    @Autowired
    private SalesService salesService;

    @Autowired
    private UserService userService;

    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<SalesSummaryDTO> getSummary(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean paidOnly
    ) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(salesService.getSalesSummary(user, paidOnly));
    }

    @GetMapping("/user/{userId}/top-products")
    public ResponseEntity<List<TopProductDTO>> getTopProducts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean paidOnly,
            @RequestParam(defaultValue = "5") int limit
    ) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(salesService.getTopProducts(user, paidOnly, limit));
    }

    @GetMapping("/user/{userId}/monthly")
    public ResponseEntity<List<PeriodSalesDTO>> getMonthlySales(
            @PathVariable Long userId,
            @RequestParam int year,
            @RequestParam(defaultValue = "false") boolean paidOnly
    ) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(salesService.getMonthlySales(user, year, paidOnly));
    }

    @GetMapping("/user/{userId}/yearly")
    public ResponseEntity<List<PeriodSalesDTO>> getYearlySales(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean paidOnly
    ) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(salesService.getYearlySales(user, paidOnly));
    }

    @GetMapping("/user/{userId}/years")
    public ResponseEntity<List<Integer>> getAvailableYears(
            @PathVariable Long userId
    ) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(salesService.getAvailableYears(user));
    }
}