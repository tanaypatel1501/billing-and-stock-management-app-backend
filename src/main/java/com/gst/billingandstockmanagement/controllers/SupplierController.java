package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.dto.SupplierDTO;
import com.gst.billingandstockmanagement.security.SecurityUtils;
import com.gst.billingandstockmanagement.services.supplier.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supplier")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @PostMapping("/save")
    public ResponseEntity<SupplierDTO> saveSupplier(@RequestBody SupplierDTO dto) {
        dto.setUserId(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(supplierService.saveSupplier(dto));
    }

    @GetMapping("/page")
    public ResponseEntity<Page<SupplierDTO>> getPaged(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return ResponseEntity.ok(supplierService.getPagedByUser(SecurityUtils.getCurrentUserId(), search, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<List<SupplierDTO>> search(@RequestParam String name) {
        return ResponseEntity.ok(supplierService.searchByName(SecurityUtils.getCurrentUserId(), name));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        supplierService.deleteSupplier(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}