package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.dto.ProductRequestDTO;
import com.gst.billingandstockmanagement.security.SecurityUtils;
import com.gst.billingandstockmanagement.services.productrequest.ProductRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product-requests")
public class ProductRequestController {

    @Autowired
    private ProductRequestService productRequestService;

    @PostMapping("/submit")
    public ResponseEntity<ProductRequestDTO> submit(@RequestBody ProductRequestDTO dto) {
        return ResponseEntity.ok(productRequestService.submitRequest(dto, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ProductRequestDTO>> myRequests() {
        return ResponseEntity.ok(productRequestService.getRequestsByUser(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductRequestDTO>> pending() {
        return ResponseEntity.ok(productRequestService.getPendingRequests());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductRequestDTO>> all() {
        return ResponseEntity.ok(productRequestService.getAllRequests());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductRequestDTO> approve(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.getOrDefault("adminNotes", "") : "";
        return ResponseEntity.ok(productRequestService.approveRequest(id, notes));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductRequestDTO> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.getOrDefault("adminNotes", "") : "";
        return ResponseEntity.ok(productRequestService.rejectRequest(id, notes));
    }
}