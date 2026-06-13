package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.dto.PurchaserDTO;
import com.gst.billingandstockmanagement.services.purchaser.PurchaserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@RestController
@RequestMapping("/api/purchaser")
public class PurchaserController {

    @Autowired
    private PurchaserService purchaserService;

    @PostMapping("/save")
    public ResponseEntity<PurchaserDTO> savePurchaser(@RequestBody PurchaserDTO dto) {
        return ResponseEntity.ok(purchaserService.savePurchaser(dto));
    }

    @GetMapping("/page")
    public ResponseEntity<Page<PurchaserDTO>> getPaged(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return ResponseEntity.ok(purchaserService.getPagedByUser(userId, search, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<List<PurchaserDTO>> search(
            @RequestParam Long userId,
            @RequestParam String name) {
        return ResponseEntity.ok(purchaserService.searchByName(userId, name));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        purchaserService.deletePurchaser(id);
        return ResponseEntity.noContent().build();
    }
}