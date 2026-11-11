package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.dto.PurchaseDTO;
import com.gst.billingandstockmanagement.security.SecurityUtils;
import com.gst.billingandstockmanagement.services.purchase.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/purchase")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @PostMapping("/add")
    public ResponseEntity<PurchaseDTO> addPurchase(@RequestBody PurchaseDTO dto) {
        dto.setUserId(SecurityUtils.getCurrentUserId());
        return new ResponseEntity<>(purchaseService.submitPurchase(dto), HttpStatus.CREATED);
    }

    @GetMapping("/page")
    public ResponseEntity<Page<PurchaseDTO>> getPage(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        // The repository query sorts by invoice date, so the pageable stays unsorted
        return ResponseEntity.ok(purchaseService.getPurchases(
                SecurityUtils.getCurrentUserId(), search, fromDate, toDate,
                PageRequest.of(Math.max(page, 0), safeSize)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.getPurchaseById(id, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/{id}/revert")
    public ResponseEntity<Void> revert(@PathVariable Long id) {
        purchaseService.revertPurchase(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok().build();
    }
}