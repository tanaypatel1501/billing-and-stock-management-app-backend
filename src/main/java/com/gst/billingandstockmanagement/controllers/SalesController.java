package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.dto.sales.PeriodSalesDTO;
import com.gst.billingandstockmanagement.dto.sales.SalesSummaryDTO;
import com.gst.billingandstockmanagement.dto.sales.TopProductDTO;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.security.SecurityUtils;
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

    @GetMapping("/user/summary")
    public ResponseEntity<SalesSummaryDTO> getSummary(
            @RequestParam(defaultValue = "false") boolean paidOnly
    ) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(salesService.getSalesSummary(user, paidOnly));
    }

    @GetMapping("/user/top-products")
    public ResponseEntity<List<TopProductDTO>> getTopProducts(
            @RequestParam(defaultValue = "false") boolean paidOnly,
            @RequestParam(defaultValue = "5") int limit
    ) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(salesService.getTopProducts(user, paidOnly, limit));
    }

    @GetMapping("/user/monthly")
    public ResponseEntity<List<PeriodSalesDTO>> getMonthlySales(
            @RequestParam int year,
            @RequestParam(defaultValue = "false") boolean paidOnly
    ) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(salesService.getMonthlySales(user, year, paidOnly));
    }

    @GetMapping("/user/yearly")
    public ResponseEntity<List<PeriodSalesDTO>> getYearlySales(
            @RequestParam(defaultValue = "false") boolean paidOnly
    ) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(salesService.getYearlySales(user, paidOnly));
    }

    @GetMapping("/user/years")
    public ResponseEntity<List<Integer>> getAvailableYears() {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(salesService.getAvailableYears(user));
    }
}